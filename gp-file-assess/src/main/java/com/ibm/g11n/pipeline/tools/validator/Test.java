package com.ibm.g11n.pipeline.tools.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Test {

    public static void main(String[] args) {
        for(String s : new Test().getLeftValues("E:\\test.xliff").values())
            System.out.println(s);
    }
    HashMap<String, String> getLeftValues(String fileName) {
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
}
