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
package com.ibm.g11n.pipeline.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
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

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Entering GPExportMojo#execute()");

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
            LanguageIdSeparator langSep = bundleSet.getLanguageIdSeparator();
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
                Set<String> bdlLangs = new HashSet<String>();
                bdlLangs.add(bdlSrcLang);
                bdlLangs.addAll(bdlData.getTargetLanguages());

                if (!srcLang.equals(bdlSrcLang)) {
                    getLog().warn("The source language of the bundle:" + bundleId
                            + " (" + bdlSrcLang + ") is different from the language specified by the configuration ("
                            + bdlSrcLang + ")");
                }

                if (outputSrcLang) {
                    if (bdlLangs.contains(srcLang)) {
                        exportLanguageResource(client, bf, srcLang, outDir,
                                outContentOpt, bundleLayout, langSep, langMap);
                    } else {
                        getLog().warn("The specified source language (" + srcLang
                                + ") does not exist in the bundle:" + bundleId);
                    }
                }

                for (String tgtLang: tgtLangs) {
                    if (bdlLangs.contains(tgtLang)) {
                        exportLanguageResource(client, bf, tgtLang, outDir,
                                outContentOpt, bundleLayout, langSep, langMap);
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
            LanguageIdSeparator langSep, Map<String, String> langMap)
            throws MojoFailureException {
        String srcFileName = bf.getFile().getName();
        String relPath = bf.getRelativePath();

        File outputFile = null;

        switch (bundleLayout) {
        case LANGUAGE_SUFFIX: {
            File dir = (new File(outputDir, relPath)).getParentFile();
            int idx = srcFileName.lastIndexOf('.');
            String tgtName = null;
            if (idx < 0) {
                tgtName = srcFileName + "_" + getLanguageId(language, langSep, langMap);
            } else {
                tgtName = srcFileName.substring(0, idx) + "_" + getLanguageId(language, langSep, langMap)
                    + srcFileName.substring(idx);
            }
            outputFile = new File(dir, tgtName);
            break;
        }
        case LANGUAGE_SUBDIR: {
            File dir = (new File(outputDir, relPath)).getParentFile();
            File langSubDir = new File(dir, getLanguageId(language, langSep, langMap));
            outputFile = new File(langSubDir, srcFileName);
            break;
        }
        case LANGUAGE_DIR:
            File dir = (new File(outputDir, relPath)).getParentFile().getParentFile();
            File langDir = new File(dir, getLanguageId(language, langSep, langMap));
            outputFile = new File(langDir, srcFileName);
            break;
        }

        if (outputFile == null) {
            throw new MojoFailureException("Failed to resolve output directory");
        }

        getLog().info("Exporting bundle:" + bf.getBundleId() + " language:" + language + " to "
                + outputFile.getAbsolutePath());

        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        switch (outContntOpt) {
        case MERGE_TO_SOURCE:
            mergeTranslation(client, bf.getBundleId(), language, bf.getType(), bf.getFile(), outputFile);
            break;
        case TRANSLATED_WITH_FALLBACK:
            exportTranslation(client, bf.getBundleId(), language, bf.getType(), outputFile, true);
            break;
        case TRANSLATED_ONLY:
            exportTranslation(client, bf.getBundleId(), language, bf.getType(), outputFile, false);
            break;
        }
    }

    private String getLanguageId(String gpLanguageTag, LanguageIdSeparator langSep,
            Map<String, String> langMap) {
        String languageId = gpLanguageTag;
        if (langMap != null) {
            String mappedId = langMap.get(gpLanguageTag);
            if (mappedId != null) {
                languageId = mappedId;
            }
        }
        switch (langSep) {
        case UNDERSCORE:
            languageId = languageId.replace('-', '_');
            break;
        case HYPHEN:
            // do nothing
            break;
        }
        return languageId;
    }

    // TODO: Some resource filter types do not support 'merge' method
    // and throwing UnsupportedOperationException at runtime. For now,
    // this implementation has hardcoded types that support 'merge' operation.
    // The resource filter API should provide a method returning if the filter
    // implementation supports the operation or not.
    private static final EnumSet<ResourceType> MERGE_AVAIL_TYPES =
            EnumSet.of(
                    ResourceType.AMDJS,
                    ResourceType.ANDROID,
                    ResourceType.IOS,
                    ResourceType.JAVA,
                    ResourceType.PO,
                    ResourceType.POT);

    private void mergeTranslation(ServiceClient client, String bundleId, String language,
            ResourceType type, File srcFile, File outFile) throws MojoFailureException {
        if (!MERGE_AVAIL_TYPES.contains(type)) {
            exportTranslation(client, bundleId, language, type, outFile, true);
            return;
        }

        Collection<ResourceString> resStrings = null;
        try {
            resStrings = getResourceStrings(client, bundleId, language, false);
        } catch (ServiceException e) {
            throw new MojoFailureException("Globalization Pipeline service error", e);
        }

        ResourceFilter filter = ResourceFilterFactory.get(type);
        try (FileOutputStream fos = new FileOutputStream(outFile);
                FileInputStream fis = new FileInputStream(srcFile)) {
            filter.merge(fis, fos, language, resStrings);
        } catch (IOException e) {
            throw new MojoFailureException("I/O error while merging the translated values to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    private void exportTranslation(ServiceClient client, String bundleId, String language,
            ResourceType type, File outFile, boolean withFallback) throws MojoFailureException {
        Collection<ResourceString> resStrings = null;
        try {
            resStrings = getResourceStrings(client, bundleId, language, withFallback);
        } catch (ServiceException e) {
            throw new MojoFailureException("Globalization Pipeline service error", e);
        }

        ResourceFilter filter = ResourceFilterFactory.get(type);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            filter.write(fos, language, resStrings);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write the translated resoruce data to "
                    + outFile.getAbsolutePath(), e);
        }
    }

    private Collection<ResourceString> getResourceStrings(ServiceClient client,
            String bundleId, String language, boolean withFallback) throws ServiceException {
        Map<String, ResourceEntryData> resEntries = client.getResourceEntries(bundleId, language);
        Collection<ResourceString> resStrings = new LinkedList<>();
        for (Entry<String, ResourceEntryData> entry : resEntries.entrySet()) {
            String key = entry.getKey();
            ResourceEntryData data = entry.getValue();
            String resVal = data.getValue();
            Integer seqNum = data.getSequenceNumber();
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
        return resStrings;
    }
}
