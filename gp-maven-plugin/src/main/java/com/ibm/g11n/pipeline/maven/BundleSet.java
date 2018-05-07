/*  
 * Copyright IBM Corp. 2016, 2018
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
package com.ibm.g11n.pipeline.maven;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;

/**
 * BundleSet class is used for specifying a set of bundle files
 * and configuration for the set.
 * 
 * @author Yoshito Umaoka
 */
public class BundleSet {
    @Parameter(required = true)
    private FileSet sourceFiles;

    @Parameter(defaultValue = "JAVA")
    private String type = "JAVA";

    @Parameter(defaultValue = "en")
    private String sourceLanguage = "en";

    @Parameter
    private Set<String> targetLanguages = null;

    // Custom language mapping. Each key is BCP 47 language tag used by
    // Globalization Pipeline service instance, and its corresponding value is
    // BCP 47 language tag used for bundle file/path name to be generated.
    @Parameter
    private Map<String, String> languageMap = null;

    @Parameter(defaultValue = "BCP47_UNDERSCORE")
    private LanguageIdStyle languageIdStyle = LanguageIdStyle.BCP47_UNDERSCORE;

    @Parameter
    private File outputDir = null;

    @Parameter(defaultValue = "false")
    private boolean outputSourceLanguage = false;

    @Parameter(defaultValue = "MERGE_TO_SOURCE")
    private OutputContentOption outputContentOption = OutputContentOption.MERGE_TO_SOURCE;

    @Parameter(defaultValue = "LANGUAGE_SUFFIX")
    private BundleLayout bundleLayout = BundleLayout.LANGUAGE_SUFFIX;

    @Parameter
    private List<RegexMapper> pathToBundleMapper;

    @Parameter
    private List<RegexMapper> sourcePathToTargetMapper;

    public BundleSet() {
    }

    public BundleSet(FileSet sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the source files
     */
    public FileSet getSourceFiles() {
        return sourceFiles;
    }

    /**
     * @param sourceFiles the source files to set
     */
    public void setSourceFiles(FileSet sourceFiles) {
        this.sourceFiles = sourceFiles;
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
        return targetLanguages;
    }

    /**
     * @param targetLanguages the targetLanguages to set
     */
    public void setTargetLanguages(Set<String> targetLanguages) {
        this.targetLanguages = targetLanguages;
    }

    /**
     * @return the languageMap
     */
    public Map<String, String> getLanguageMap() {
        return languageMap;
    }

    /**
     * @param languageMap the languageMap to set
     */
    public void setLanguageMap(Map<String, String> languageMap) {
        this.languageMap = languageMap;
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
     * @return the pathToBundleMapper
     */
    public List<RegexMapper> getPathToBundleMapper() {
        return pathToBundleMapper;
    }

    /**
     * @param pathToBundleMapper the pathToBundleMapper to set
     */
    public void setPathToBundleMapper(List<RegexMapper> pathToBundleMapper) {
        this.pathToBundleMapper = pathToBundleMapper;
    }

    /**
     * @return the sourcePathToTargetMapper
     */
    public List<RegexMapper> getSourcePathToTargetMapper() {
        return sourcePathToTargetMapper;
    }

    /**
     * @param sourcePathToTargetMapper the sourcePathToTargetMapper to set
     */
    public void setSourcePathToTargetMapper(List<RegexMapper> sourcePathToTargetMapper) {
        this.sourcePathToTargetMapper = sourcePathToTargetMapper;
    }
}
