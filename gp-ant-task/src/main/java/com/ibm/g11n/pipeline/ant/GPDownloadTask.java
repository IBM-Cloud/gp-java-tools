/*
 * Copyright IBM Corp. 2017, 2018
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.ResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.FilterOptions;
import com.ibm.g11n.pipeline.resfilter.LanguageBundle;
import com.ibm.g11n.pipeline.resfilter.LanguageBundleBuilder;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterException;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;

/**
 * Fetches translated string resource bundles from an instance of
 * Globalization Pipeline service and produces bundle files.
 * 
 * @author Yoshito Umaoka
 */
public class GPDownloadTask extends GPBaseTask {
    /**
     * Base directory of the output files. When &lt;bundleSet&gt; configuration
     * does not contain &lt;outputDir&gt; element, then this configuration
     * is used for the bundleSet.
     */
    private File outputDir;

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Whether this goal overwrites existing bundle file in output directory
     * or not. The default value is true.
     */
    private boolean overwrite = true;

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * The execution task - downloading bundle(s) from the globalization
     * pipeline service instance 
     */
    @Override
    public void execute() throws BuildException {
        getProject().log("Entering GPDownloadTask#execute()", Project.MSG_DEBUG);

        ServiceClient client = getServiceClient();

        Set<String> availBundleIds = null;
        try {
            availBundleIds = client.getBundleIds();
        } catch (ServiceException e) {
            throw new BuildException("Failed to get available bundle IDs.", e);
        }
        List<BundleSet> bundleSets = null;
        try {
            bundleSets = getBundleSets();
        } catch (FileNotFoundException e) {
            throw new BuildException("Source directory not found/specified!", e);
        }
        for (BundleSet bundleSet : bundleSets) {
            String srcLang = bundleSet.getSourceLanguage();
            Set<String> tgtLangs = resolveTargetLanguages(bundleSet);
            boolean outputSrcLang = bundleSet.isOutputSourceLanguage();
            List<SourceBundleFile> sourceBundleFiles = getSourceBundleFiles(bundleSet);
            OutputContentOption outContentOpt = bundleSet.getOutputContentOption();
            BundleLayout bundleLayout = bundleSet.getBundleLayout();
            LanguageIdStyle langIdStyle = bundleSet.getLanguageIdStyle();
            List<LanguageMap> langMapList = bundleSet.getLanguageMap();
            Map<String, String> langMap = new HashMap<String, String>();
            for (LanguageMap languageMap : langMapList) {
                langMap.put(languageMap.getFrom(), languageMap.getTo());
            }

            File outDir = bundleSet.getOutputDir();
            if (outDir == null) {
                if (outputDir == null) {
                    throw new BuildException("Output Directory not found/specified!");
                }
                outDir = outputDir;
            }
            if (outDir.exists()) {
                outDir.mkdirs();
            }
            for (SourceBundleFile bf : sourceBundleFiles) {
                String bundleId = bf.getBundleId();

                if (!availBundleIds.contains(bundleId)) {
                    getProject().log("The bundle:" + bundleId + " does not exist.", Project.MSG_WARN);
                    continue;
                }

                BundleData bdlData = null;
                try {
                    bdlData = client.getBundleInfo(bundleId);
                } catch (ServiceException e) {
                    throw new BuildException("Failed to get bundle data for " + bundleId, e);
                }

                String bdlSrcLang = bdlData.getSourceLanguage();
                Set<String> bdlLangs = new HashSet<String>();
                bdlLangs.add(bdlSrcLang);
                bdlLangs.addAll(bdlData.getTargetLanguages());

                if (!srcLang.equals(bdlSrcLang)) {
                    getProject().log("The source language of the bundle:" + bundleId
                            + " (" + bdlSrcLang + ") is different from the language specified by the configuration ("
                            + bdlSrcLang + ")", Project.MSG_WARN);
                }

                if (outputSrcLang) {
                    if (bdlLangs.contains(srcLang)) {
                        exportLanguageResource(client, bf, srcLang, outDir,
                                outContentOpt, bundleLayout, langIdStyle, langMap);
                    } else {
                        getProject().log("The specified source language (" + srcLang
                                + ") does not exist in the bundle:" + bundleId, Project.MSG_WARN);
                    }
                }

                for (String tgtLang: tgtLangs) {
                    if (bdlLangs.contains(tgtLang)) {
                        exportLanguageResource(client, bf, tgtLang, outDir,
                                outContentOpt, bundleLayout, langIdStyle, langMap);
                    } else {
                        getProject().log("The specified target language (" + tgtLang
                                + ") does not exist in the bundle:" + bundleId, Project.MSG_WARN);
                    }
                }
            }
        }
    }

    /**
     * This function defines various ways in which the downloadable bundle files can be organized
     * @param client
     * @param bf
     * @param language
     * @param outBaseDir
     * @param outContntOpt
     * @param bundleLayout
     * @param langIdStyle
     * @param langMap
     * @throws BuildException
     */
    private void exportLanguageResource(ServiceClient client, SourceBundleFile bf, String language,
            File outBaseDir, OutputContentOption outContntOpt, BundleLayout bundleLayout,
            LanguageIdStyle langIdStyle, Map<String, String> langMap)
                    throws BuildException {
        String srcFileName = bf.getFile().getName();
        String relPath = bf.getRelativePath();

        File outputFile = null;

        switch (bundleLayout) {
        case LANGUAGE_SUFFIX: {
            File dir = (new File(outBaseDir, relPath)).getParentFile();
            int idx = srcFileName.lastIndexOf('.');
            String tgtName = null;
            if (idx < 0) {
                tgtName = srcFileName + "_" + getLanguageId(language, langIdStyle, langMap);
            } else {
                tgtName = srcFileName.substring(0, idx) + "_" + getLanguageId(language, langIdStyle, langMap)
                + srcFileName.substring(idx);
            }
            outputFile = new File(dir, tgtName);
            break;
        }
        case LANGUAGE_SUBDIR: {
            File dir = (new File(outBaseDir, relPath)).getParentFile();
            File langSubDir = new File(dir, getLanguageId(language, langIdStyle, langMap));
            outputFile = new File(langSubDir, srcFileName);
            break;
        }
        case LANGUAGE_DIR:
            File dir = (new File(outBaseDir, relPath)).getParentFile().getParentFile();
            File langDir = new File(dir, getLanguageId(language, langIdStyle, langMap));
            outputFile = new File(langDir, srcFileName);
            break;
        }

        if (outputFile == null) {
            throw new BuildException("Failed to resolve output directory");
        }

        getProject().log("Exporting bundle:" + bf.getBundleId() + " language:" + language + " to "
                + outputFile.getAbsolutePath(), Project.MSG_INFO);

        if (outputFile.exists()) {
            if (overwrite) {
                getProject().log("The output bundle file:" + outputFile.getAbsolutePath()
                + " already exists - overwriting", Project.MSG_INFO);
            } else {
                getProject().log("The output bundle file:" + outputFile.getAbsolutePath()
                + " already exists - skipping", Project.MSG_INFO);
                // When overwrite is false, do nothing
                return;
            }
        }

        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        LanguageBundle bundle;
        String embeddedLangId = getEmbeddedLanguageId(language, langMap);

        switch (outContntOpt) {
        case MERGE_TO_SOURCE:
            bundle = getBundle(client, bf.getBundleId(), language, embeddedLangId, false, true);
            mergeTranslation(bundle, language, bf.getType(), bf.getFile(), outputFile);
            break;

        case TRANSLATED_WITH_FALLBACK:
            bundle = getBundle(client, bf.getBundleId(), language, embeddedLangId, false, true);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;

        case TRANSLATED_ONLY:
            bundle = getBundle(client, bf.getBundleId(), language, embeddedLangId, false, false);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;

        case MERGE_REVIEWED_TO_SOURCE:
            bundle = getBundle(client, bf.getBundleId(), language, embeddedLangId, true, true);
            mergeTranslation(bundle, language, bf.getType(), bf.getFile(), outputFile);
            break;

        case REVIEWED_WITH_FALLBACK:
            bundle = getBundle(client, bf.getBundleId(), language, embeddedLangId, true, true);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;

        case REVIEWED_ONLY:
            bundle = getBundle(client, bf.getBundleId(), language, embeddedLangId, true, false);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;
        }
    }

    private String getLanguageId(String gpLanguageTag, LanguageIdStyle langIdStyle,
            Map<String, String> langMap) {
        String languageId = gpLanguageTag;
        if (langMap != null) {
            String mappedId = langMap.get(gpLanguageTag);
            if (mappedId != null) {
                languageId = mappedId;
            }
        }
        switch (langIdStyle) {
        case BCP47_UNDERSCORE:
            languageId = languageId.replace('-', '_');
            break;
        case BCP47:
            // do nothing
            break;
        }
        return languageId;
    }

    private String getEmbeddedLanguageId(String gpLanguageTag, Map<String, String> langMap) {
        String languageId = gpLanguageTag;
        if (langMap != null) {
            String mappedId = langMap.get(gpLanguageTag);
            if (mappedId != null) {
                languageId = mappedId;
            }
        }
        return languageId;
    }

    private void mergeTranslation(LanguageBundle bundle, String language, String type,
            File srcFile, File outFile) throws BuildException {
        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(type);
        if (filter == null) {
            throw new BuildException("Unknown resource filter type - " + type);
        }
        try (FileOutputStream fos = new FileOutputStream(outFile);
                FileInputStream fis = new FileInputStream(srcFile)) {
            filter.merge(fis, fos, bundle, new FilterOptions(Locale.forLanguageTag(language)));
        } catch (IOException e) {
            throw new BuildException("I/O error while merging the translated strings to "
                    + outFile.getAbsolutePath(), e);
        } catch (ResourceFilterException e) {
            throw new BuildException("Resource filter error while merging the translated strings to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    private void exportTranslation(LanguageBundle bundle, String language, String type,
            File outFile) throws BuildException {
        ResourceFilter filter = ResourceFilterFactory.getResourceFilter(type);
        if (filter == null) {
            throw new BuildException("Unknown resource filter type - " + type);
        }
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            filter.write(fos, bundle, new FilterOptions(Locale.forLanguageTag(language)));
        } catch (IOException e) {
            throw new BuildException("I/O error while writing the translated strings to "
                    + outFile.getAbsolutePath(), e);
        } catch (ResourceFilterException e) {
            throw new BuildException("Resource filter error while writing the translated strings to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    private LanguageBundle getBundle(ServiceClient client, String bundleId, String language, String embeddedLanguageId,
            boolean reviewedOnly, boolean withFallback) throws BuildException {
        try {
            LanguageBundleBuilder bundleBuilder = new LanguageBundleBuilder(false);
            bundleBuilder.embeddedLanguageCode(embeddedLanguageId);

            Map<String, ResourceEntryData> resEntries = client.getResourceEntries(bundleId, language);
            for (Entry<String, ResourceEntryData> entry : resEntries.entrySet()) {
                String key = entry.getKey();
                ResourceEntryData data = entry.getValue();
                String resVal = data.getValue();
                String srcVal = data.getSourceValue();
                Integer seqNum = data.getSequenceNumber();
                List<String> notes = data.getNotes();

                if (reviewedOnly) {
                    if (!data.isReviewed()) {
                        resVal = null;
                    }
                }

                if (resVal == null && withFallback) {
                    resVal = data.getSourceValue();
                }

                if (resVal != null) {
                    ResourceString.Builder resb = ResourceString.with(key, resVal).sourceValue(srcVal);
                    if (seqNum != null) {
                        resb.sequenceNumber(seqNum.intValue());
                    }
                    if (notes != null) {
                        resb.notes(notes);
                    }
                    bundleBuilder.addResourceString(resb);
                }
            }
            return bundleBuilder.build();
        } catch (ServiceException e) {
            throw new BuildException("Globalization Pipeline service error", e);
        }
    }
}
