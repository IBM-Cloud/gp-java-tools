/*  
 * Copyright IBM Corp. 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.g11n.pipeline.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.types.FileSet;

import com.ibm.g11n.pipeline.resfilter.ResourceType;

/**
 * BundleSet class is used for specifying a set of bundle files
 * and configuration for the set.
 * 
 * @author Yoshito Umaoka
 */
public class BundleSet {

    private ResourceType type = ResourceType.JAVA;

    private String sourceLanguage = "en";

    private Set<String> targetLanguages = null;

    // Custom language mapping. Each key is BCP 47 language tag used by
    // Globalization Pipeline service instance, and its corresponding value is
    // BCP 47 language tag used for bundle file/path name to be generated.
    private List<LanguageMap> languageMapList = new ArrayList<LanguageMap>();

    private LanguageIdStyle languageIdStyle = LanguageIdStyle.BCP47_UNDERSCORE;

    private File outputDir = null;

    private boolean outputSourceLanguage = false;

    private OutputContentOption outputContentOption = OutputContentOption.MERGE_TO_SOURCE;

    private BundleLayout bundleLayout = BundleLayout.LANGUAGE_SUFFIX;


    public BundleSet() {
    }

    public BundleSet(FileSet sourceFiles) {
        fileSets.add(sourceFiles);
    }

    /**
     * @return the type
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(ResourceType type) {
        this.type = type;
    }

    /**
     * @return the source files
     */
    public FileSet getSourceFiles() {
        return fileSets.get(0);
    }


    /**
     * @return the sourceLanguage
     */
    public String getSourceLanguage() {
        return sourceLanguage;
    }

    /**
     * @param sourceLanguage the sourceLanguage to set
     */
    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    /**
     * @return the targetLanguages
     */
    public Set<String> getTargetLanguages() {
        if (targetLanguages != null)
            return targetLanguages;
        targetLanguages = new HashSet<String>();
        for (TargetLanguage tl : targetLangs) {
            targetLanguages.add(tl.getLang());
        }
        return targetLanguages;
    }

    /**
     * @return the languageMapList
     */
    public List<LanguageMap> getLanguageMap() {
        return languageMapList;
    }

    /**
     * @param languageMap the languageMaps to set
     */
    public void addLanguageMap(LanguageMap languageMap) {
        languageMapList.add(languageMap);
    }

    /**
     * @return the languageIdStyle
     */
    public LanguageIdStyle getLanguageIdStyle() {
        return languageIdStyle;
    }

    /**
     * @param languageIdStyle the languageIdStyle to set
     */
    public void setLanguageIdStyle(LanguageIdStyle languageIdStyle) {
        this.languageIdStyle = languageIdStyle;
    }

    /**
     * @return the outputDir
     */
    public File getOutputDir() {
        return outputDir;
    }

    /**
     * @param outputDir the outputDir to set
     */
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * @return the outputSourceLanguage
     */
    public boolean isOutputSourceLanguage() {
        return outputSourceLanguage;
    }

    /**
     * @param outputSourceLanguage the outputSourceLanguage to set
     */
    public void setOutputSourceLanguage(boolean outputSourceLanguage) {
        this.outputSourceLanguage = outputSourceLanguage;
    }

    /**
     * @return the outputContentOption
     */
    public OutputContentOption getOutputContentOption() {
        return outputContentOption;
    }

    /**
     * @param outputContentOption the outputContentOption to set
     */
    public void setOutputContentOption(OutputContentOption outputContentOption) {
        this.outputContentOption = outputContentOption;
    }

    /**
     * @return the bundle layout
     */
    public BundleLayout getBundleLayout() {
        return bundleLayout;
    }

    /**
     * @param bundleLayout the bundle layout to set
     */
    public void setBundleLayout(BundleLayout bundleLayout) {
        this.bundleLayout = bundleLayout;
    }

    /**
     * A set of TargetLanguage to supported nested elements for ant script
     */
    protected Set<TargetLanguage> targetLangs = new HashSet<TargetLanguage>();

    /**
     * Supporting code for nested element of targetLanguage as required for custom ant build script
     * @return
     */
    public void addTargetLanguage(TargetLanguage tl) {
        targetLangs.add(tl);
    }

    protected List<FileSet> fileSets = new ArrayList<FileSet>();

    public void addFileset(FileSet fileSet) {
        fileSets.add(fileSet);
    }

}
