package com.ibm.g11n.pipeline.tools.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.ibm.g11n.pipeline.resfilter.impl.BomInputStream;

public class JsonValidator extends BaseValidator {
    
    public JsonValidator(File tba_file, String type) {
        super(tba_file, type);
    }

    private boolean check_duplicated_key() {
        ArrayList<String> kl= new ArrayList<String>();
        Pattern r = Pattern.compile(this.kvPattern);
        Matcher m = r.matcher(this.content);
        while(m.find()) {
            String key = m.group(1);
            if(kl.contains(key)) {
                System.err.println("Failed - Duplicated key found in json file, please remove them and try again: " + m.group(1));
                return false;
            } else {
                kl.add(key);
            }
        }
        System.out.println("Pass - Not found duplicated keys in JSON file");
        return true;
    }
    
    private boolean checkRootElement() {
        try (InputStreamReader reader = new InputStreamReader(new BomInputStream(new FileInputStream(this.tba_file)), StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            if (!root.isJsonObject()) {
                System.err.println("Failed - The root element is not a Json Object, please fix it and try again!");
                return false;
            }
        } catch (JsonParseException e) {
            System.err.println("Failed - Failed to parse the specified JSON contents.");
            e.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        System.out.println("Pass - The root object is JSON Object");
        return true;
    }

    @Override
    protected boolean preCheck() {
        return this.check_duplicated_key() && this.checkRootElement();
    }
}
