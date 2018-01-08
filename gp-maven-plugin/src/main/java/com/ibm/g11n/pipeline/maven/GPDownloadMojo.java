/*
 * Copyright IBM Corp. 2016, 2017
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.ResourceEntryData;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.Bundle;
import com.ibm.g11n.pipeline.resfilter.ResourceFilter;
import com.ibm.g11n.pipeline.resfilter.ResourceFilterFactory;
import com.ibm.g11n.pipeline.resfilter.ResourceString;
import com.ibm.g11n.pipeline.resfilter.ResourceType;

/**
 * Fetches translated string resource bundles from an instance of
 * Globalization Pipeline service and produces bundle files.
 * 
 * @author Yoshito Umaoka
 */
@Mojo(name = "download", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GPDownloadMojo extends GPBaseMojo {
    /**
     * Base directory of the output files. When &lt;bundleSet&gt; configuration
     * does not contain &lt;outputDir&gt; element, then this configuration
     * is used for the bundleSet.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File outputDir;

    /**
     * Whether this goal overwrites existing bundle file in output directory
     * or not. The default value is true.
     */
    @Parameter(defaultValue = "true")
    private boolean overwrite;

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Entering GPDownloadMojo#execute()");

        ServiceClient client = getServiceClient();

        Set<String> availBundleIds = null;
        try {
            availBundleIds = client.getBundleIds();
        } catch (ServiceException e) {
            throw new MojoFailureException("Failed to get available bundle IDs.", e);
        }

        List<BundleSet> bundleSets = getBundleSets();
        for (BundleSet bundleSet : bundleSets) {
            String srcLang = bundleSet.getSourceLanguage();
            Set<String> tgtLangs = resolveTargetLanguages(bundleSet);
            boolean outputSrcLang = bundleSet.isOutputSourceLanguage();
            List<SourceBundleFile> sourceBundleFiles = getSourceBundleFiles(bundleSet);
            OutputContentOption outContentOpt = bundleSet.getOutputContentOption();
            BundleLayout bundleLayout = bundleSet.getBundleLayout();
            LanguageIdStyle langIdStyle = bundleSet.getLanguageIdStyle();
            Map<String, String> langMap = bundleSet.getLanguageMap();

            File outDir = bundleSet.getOutputDir();
            if (outDir == null) {
                outDir = outputDir;
            }
            if (outDir.exists()) {
                outDir.mkdirs();
            }

            for (SourceBundleFile bf : sourceBundleFiles) {
                String bundleId = bf.getBundleId();
                if (!availBundleIds.contains(bundleId)) {
                    getLog().warn("The bundle:" + bundleId + " does not exist.");
                    continue;
                }

                BundleData bdlData = null;
                try {
                    bdlData = client.getBundleInfo(bundleId);
                } catch (ServiceException e) {
                    throw new MojoFailureException("Failed to get bundle data for " + bundleId, e);
                }

                String bdlSrcLang = bdlData.getSourceLanguage();
                Set<String> bdlTrgLangs = bdlData.getTargetLanguages();
                Set<String> bdlLangs = new HashSet<String>();
                bdlLangs.add(bdlSrcLang);
                if (bdlTrgLangs != null) {
                    bdlLangs.addAll(bdlData.getTargetLanguages());
                }

                if (!srcLang.equals(bdlSrcLang)) {
                    getLog().warn("The source language of the bundle:" + bundleId
                            + " (" + bdlSrcLang + ") is different from the language specified by the configuration ("
                            + bdlSrcLang + ")");
                }

                if (outputSrcLang) {
                    if (bdlLangs.contains(srcLang)) {
                        exportLanguageResource(client, bf, srcLang, outDir,
                                outContentOpt, bundleLayout, langIdStyle, langMap, srcLang);
                    } else {
                        getLog().warn("The specified source language (" + srcLang
                                + ") does not exist in the bundle:" + bundleId);
                    }
                }

                for (String tgtLang: tgtLangs) {
                    if (bdlLangs.contains(tgtLang)) {
                        exportLanguageResource(client, bf, tgtLang, outDir,
                                outContentOpt, bundleLayout, langIdStyle, langMap, srcLang);
                    } else {
                        getLog().warn("The specified target language (" + tgtLang
                                + ") does not exist in the bundle:" + bundleId);
                    }
                }
            }
        }
    }

    private void exportLanguageResource(ServiceClient client, SourceBundleFile bf, String language,
            File outBaseDir, OutputContentOption outContntOpt, BundleLayout bundleLayout,
            LanguageIdStyle langIdStyle, Map<String, String> langMap, String srcLang)
            throws MojoFailureException {
        String srcFileName = bf.getFile().getName();
        String relPath = bf.getRelativePath();
        File outputFile = null;

        switch (bundleLayout) {
        case LANGUAGE_SUFFIX: {
            File dir = (new File(outBaseDir, relPath)).getParentFile();
            
            // truncate source suffix from sourceFile name - BEGIN
            int extensionIndex = srcFileName.lastIndexOf('.');
            String extension = (extensionIndex > 0) ? srcFileName.substring(extensionIndex) : "";
            int srcSuffixIndex = srcFileName.lastIndexOf("_" + getLanguageId(srcLang, langIdStyle, langMap));
            srcFileName = (srcSuffixIndex > 0) ? srcFileName.substring(0,srcSuffixIndex) + extension : srcFileName;
            // truncate source suffix from sourceFile name - END
            
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
            throw new MojoFailureException("Failed to resolve output directory");
        }

        getLog().info("Exporting bundle:" + bf.getBundleId() + " language:" + language + " to "
                + outputFile.getAbsolutePath());

        if (outputFile.exists()) {
            if (overwrite) {
                getLog().info("The output bundle file:" + outputFile.getAbsolutePath()
                    + " already exists - overwriting");
            } else {
                getLog().info("The output bundle file:" + outputFile.getAbsolutePath()
                    + " already exists - skipping");
                // When overwrite is false, do nothing
                return;
            }
        }

        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        Bundle bundle;

        switch (outContntOpt) {
        case MERGE_TO_SOURCE:
            bundle = getBundle(client, bf.getBundleId(), language, false, true);
            mergeTranslation(bundle, language, bf.getType(), bf.getFile(), outputFile);
            break;

        case TRANSLATED_WITH_FALLBACK:
            bundle = getBundle(client, bf.getBundleId(), language, false, true);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;

        case TRANSLATED_ONLY:
            bundle = getBundle(client, bf.getBundleId(), language, false, false);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;

        case MERGE_REVIEWED_TO_SOURCE:
            bundle = getBundle(client, bf.getBundleId(), language, true, true);
            mergeTranslation(bundle, language, bf.getType(), bf.getFile(), outputFile);
            break;

        case REVIEWED_WITH_FALLBACK:
            bundle = getBundle(client, bf.getBundleId(), language, true, true);
            exportTranslation(bundle, language, bf.getType(), outputFile);
            break;

        case REVIEWED_ONLY:
            bundle = getBundle(client, bf.getBundleId(), language, true, false);
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

    private void mergeTranslation(Bundle bundle, String language, ResourceType type,
            File srcFile, File outFile) throws MojoFailureException {
        ResourceFilter filter = ResourceFilterFactory.get(type);
        try (FileOutputStream fos = new FileOutputStream(outFile);
                FileInputStream fis = new FileInputStream(srcFile)) {
            filter.merge(fis, fos, language, bundle);
        } catch (IOException e) {
            throw new MojoFailureException("I/O error while merging the translated values to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    private void exportTranslation(Bundle bundle, String language, ResourceType type,
            File outFile) throws MojoFailureException {
        ResourceFilter filter = ResourceFilterFactory.get(type);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            filter.write(fos, language, bundle);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write the translated resoruce data to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    private Bundle getBundle(ServiceClient client, String bundleId, String language,
            boolean reviewedOnly, boolean withFallback) throws MojoFailureException {
        try {
            Map<String, ResourceEntryData> resEntries = client.getResourceEntries(bundleId, language);
            Collection<ResourceString> resStrings = new LinkedList<>();
            for (Entry<String, ResourceEntryData> entry : resEntries.entrySet()) {
                String key = entry.getKey();
                ResourceEntryData data = entry.getValue();
                String resVal = data.getValue();
                Integer seqNum = data.getSequenceNumber();

                if (reviewedOnly) {
                    if (!data.isReviewed()) {
                        resVal = null;
                    }
                }

                if (resVal == null && withFallback) {
                    resVal = data.getSourceValue();
                }

                if (resVal != null) {
                    ResourceString resString = new ResourceString(key, resVal);
                    if (seqNum != null) {
                        resString.setSequenceNumber(seqNum.intValue());
                    }
                    resStrings.add(resString);
                }
            }
            return new Bundle(resStrings, null);
        } catch (ServiceException e) {
            throw new MojoFailureException("Globalization Pipeline service error", e);
        }
    }
}
