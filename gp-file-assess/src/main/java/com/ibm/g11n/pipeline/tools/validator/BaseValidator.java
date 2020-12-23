package com.ibm.g11n.pipeline.tools.validator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.tools.cli.GPCmd;

public class BaseValidator {
    protected File tba_file;
    protected String content;
    protected String type;
    protected ResourceFilter filter;
    // regx pattern represent key-value pairs, which is for counting PII items
    protected String kvPattern;
    // regx pattern to highlight potential unprotected strings in PII value
    protected String pupPattern;
    protected String bundleId;
    protected String xliffFile;

    public BaseValidator(File tba_file, String type) {
        this.tba_file = tba_file;
        this.type = type;
        this.content = this.getFileContent(this.tba_file);
        this.filter = ResourceFilterFactory.getResourceFilter(type);
        this.kvPattern = "\"(.*?)\" {0,3}:\\s{0,20}\"(.*?)\"";
        this.pupPattern = "!.+$|[\\$@#&]|%.*?%|\\(\\(.*?\\)\\)|\\[\\[\\.*?]\\]";
        this.bundleId = "bundle." + System.currentTimeMillis();
        this.xliffFile = System.getProperty("user.dir") + System.getProperty("file.separator") + this.bundleId
                + ".xliff";
    }

    protected void tryUpload(String jsonCreds) {
        // java -jar gp-cli.jar create -b MyNewBundle -l en,fr,de -j
        // mycreds.json

        String[] createCmd = { "create", "-b", this.bundleId, "-l", "en", "-j", jsonCreds };
        GPCmd.main(createCmd);

        // java -jar gp-cli.jar import -b MyBundle -l en -t JAVA -f
        // MyBundle.properties -j mycreds.json
        String[] uploadCmd = { "import", "-b", this.bundleId, "-l", "en", "-t", this.type, "-f",
                this.tba_file.getAbsolutePath(), "-j", jsonCreds };
        GPCmd.main(uploadCmd);
    }

    protected boolean downloadXliff(ServiceClient gpClient) {
        boolean result = true;
        Set<String> bundleIds = new HashSet<String>();
        bundleIds.add(this.bundleId);
        ByteArrayOutputStream outputXliff = new ByteArrayOutputStream();
        try (FileWriter fw = new FileWriter(xliffFile)) {
            gpClient.getXliffFromBundles("en", "en", bundleIds, outputXliff);
            byte[] xliffBytes = outputXliff.toByteArray();
            String xliff = new String(xliffBytes, StandardCharsets.UTF_8);
            fw.append(xliff);
            fw.flush();
            System.out.println("Pass - Download English XLIFF file sucessfully: " + xliffFile);
        } catch (ServiceException | IOException e) {
            result = false;
            e.printStackTrace();
        }
        return result;
    }

    protected String getFileContent(File file) {
        String encoding = "UTF-8";
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }

    protected int countPattern(String str, String pattern) {
        int count = 0;
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        while (m.find())
            count++;
        return count;
    }

    protected void checkPIICount() {
        int countInSource = countPattern(this.content, this.kvPattern);
        int countInXliff = countPattern(getFileContent(new File(this.xliffFile)), "<unit id=");

        if (countInSource == countInXliff)
            System.out.println("Pass - PII count checked");
        else
            System.err.println("Failed - PII count mismatch, please investigate: " + countInSource + ":" + countInXliff);
    }

    public void check(String jsonCreds, ServiceClient gpClient) {
        if (this.preCheck()) {
            this.tryUpload(jsonCreds);
            this.downloadXliff(gpClient);
            this.checkPIICount();
            this.printPotentialUnprotected();
        }
    }

    protected void printPotentialUnprotected() {
        HashMap<String, String> lkv = this.getLeftValues(this.xliffFile);
        boolean log = false;
        for(String value: lkv.values()) {
            Pattern r = Pattern.compile(this.pupPattern);
            Matcher m = r.matcher(value);
            if (m.find()) {
                if (log == false) {
                    System.err.println("Failed - Please check below strings to identify unprotected patterns");
                    log = true;
                }
                System.out.println(value);
            }
        }
    }

    protected HashMap<String, String> getLeftValues(String fileName) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        XliffHandler xh = new XliffHandler();
        SAXParser saxParser;
        try {
            saxParser = factory.newSAXParser();
            saxParser.parse(fileName, xh);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xh.kvMap;
    }

    class XliffHandler extends DefaultHandler {
        HashMap<String, String> kvMap = new HashMap<String, String>();
        String keyPrefix = "";
        String key = "";
        ArrayList<String> value = new ArrayList<String>();
        boolean content = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (qName.equals("file")) {
                this.keyPrefix = attributes.getValue("id");
            } else if (qName.equals("unit")) {
                this.key = attributes.getValue("name");
                if (this.key == null)
                    this.key = attributes.getValue("id");
                this.content = true;
            } else if (qName.equals("data")) {
                this.content = false;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if (qName.equals("unit")) {
                String k = this.keyPrefix + "." + this.key;
                String v = "";
                for(String s : this.value) {
                    v += s;
                }
                this.kvMap.put(k, v);
                this.value.clear();
            } else if (qName.equals("data")) {
                this.content = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            if (this.content) {
                String cv = new String(ch,start,length).trim();
                this.value.add(cv);
            }
        }
    }

    protected boolean preCheck() {
        return true;
    };
}
