package com.ibm.g11n.pipeline.resfilter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Bundle {
    private ArrayList<String> notes;
    private LinkedList<ResourceString> resStrings;
    
    public Bundle () {
        notes = new ArrayList<String>();
        resStrings = new LinkedList<ResourceString>();
    }
    
    public void addResourceString(String key, String value, int sequenceNumber) {
        ResourceString newString = new ResourceString(key, value, sequenceNumber);
        resStrings.add(newString);
    }

    public void addNote(String note) {
        notes.add(note);
    }
    
    public List<ResourceString> getResourceStrings() {
        return resStrings;
    }
    
    public List<String> getNotes() {
        return notes;
    }
}
