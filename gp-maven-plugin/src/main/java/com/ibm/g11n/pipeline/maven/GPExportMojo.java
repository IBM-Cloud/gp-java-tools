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
 * GPExportMojo is a class used for exporting the translated resource bundle
 * contents from the Globalization Pipeline service instance and producing
 * translated resource bundle files in the specified format and directory.
 * 
 * @author Yoshito Umaoka
 */
@Mojo(name = "export", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GPExportMojo extends GPBaseMojo {

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File outputDir;

    @Parameter(defaultValue = "false")
    private boolean exportSourceLanguage;

    @Parameter
    private Map<String, String> languageMap;

    public enum OutputContentOption {
        // Merges translated resources into source file contents
        // if possible. If the output resource format does not support
        // this option, TRANSLATION_WITH_FALLBACK is used.
        MERGE_TO_SOURCE,

        // Exports translated resources. If translation is not available
        // for a resource key, the value from the source bundle is used.
        TRANSLATED_WITH_FALLBACK,

        // Exports only translated resources.
        TRANSLATED_ONLY
    }

    @Parameter(defaultValue = "MERGE_TO_SOURCE")
    private OutputContentOption outputContentOption;

    public enum OutputPathOption {
        // In the same directory with the source, with extra language suffix.
        // For example, if the source file is com/ibm/g11n/MyMessages.properties,
        // then the French version will be com/ibm/g11n/MyMessages_fr.properties.
        LANGUAGE_SUFFIX,

        // In a language sub-directory of the director where the source file is.
        // For example, if the source file is com/ibm/g11n/MyMessages.json,
        // then the French version will be com/ibm/g11n/fr/MyMessages.json.
        LANGUAGE_SUBDIR,

        // In a language directory at the same level with the source file.
        // For example, if the source file is com/ibm/g11n/en/MyMessages.properties,
        // then the French version will be com/ibm/g11n/fr/MyMessages.properties.
        LANGUAGE_DIR
    };

    @Parameter(defaultValue = "LANGUAGE_SUFFIX")
    private OutputPathOption outputPathOption;

    public enum LanguageIdSeparator {
        // Use '_' as language ID separator, such as "pt_BR"
        UNDERSCORE,

        // Use '-' as language ID separator, such as "pt-BR"
        HYPHEN
    }

    @Parameter(defaultValue = "UNDERSCORE")
    private LanguageIdSeparator languageIdSeparator;

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Entering GPExportMojo#execute()");

        List<BundleFile> sourceBundleFiles = getSourceBundleFiles();
        ServiceClient client = getServiceClient();
        String srcLang = getSourceLanguage();
        Set<String> tgtLangs = getTargetLanguages();

        if (outputDir.exists()) {
            outputDir.mkdirs();
        }

        Set<String> availBundleIds = null;
        try {
            availBundleIds = client.getBundleIds();
        } catch (ServiceException e) {
            throw new MojoFailureException("Failed to get available bundle IDs.", e);
        }

        for (BundleFile bf : sourceBundleFiles) {
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

            if (exportSourceLanguage) {
                if (bdlLangs.contains(srcLang)) {
                    exportLanguageResource(client, bf, srcLang);
                } else {
                    getLog().warn("The specified source language (" + srcLang
                            + ") does not exist in the bundle:" + bundleId);
                }
            }

            for (String tgtLang: tgtLangs) {
                if (bdlLangs.contains(tgtLang)) {
                    exportLanguageResource(client, bf, tgtLang);
                } else {
                    getLog().warn("The specified target language (" + tgtLang
                            + ") does not exist in the bundle:" + bundleId);
                }
            }
        }
    }

    private void exportLanguageResource(ServiceClient client, BundleFile bf, String language)
            throws MojoFailureException {
        String srcFileName = bf.getFile().getName();
        String relPath = bf.getRelativePath();

        File outputFile = null;

        switch (outputPathOption) {
        case LANGUAGE_SUFFIX: {
            File dir = (new File(outputDir, relPath)).getParentFile();
            int idx = srcFileName.lastIndexOf('.');
            String tgtName = null;
            if (idx < 0) {
                tgtName = srcFileName + "_" + getLanguageId(language);
            } else {
                tgtName = srcFileName.substring(0, idx) + "_" + getLanguageId(language)
                    + srcFileName.substring(idx);
            }
            outputFile = new File(dir, tgtName);
            break;
        }
        case LANGUAGE_SUBDIR: {
            File dir = (new File(outputDir, relPath)).getParentFile();
            File langSubDir = new File(dir, getLanguageId(language));
            outputFile = new File(langSubDir, srcFileName);
            break;
        }
        case LANGUAGE_DIR:
            File dir = (new File(outputDir, relPath)).getParentFile().getParentFile();
            File langDir = new File(dir, getLanguageId(language));
            outputFile = new File(langDir, srcFileName);
            break;
        }

        getLog().info("Exporting bundle:" + bf.getBundleId() + " language:" + language + " to "
                + outputFile.getAbsolutePath());

        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        switch (outputContentOption) {
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

    private String getLanguageId(String languageTag) {
        String languageId = languageTag;
        switch (languageIdSeparator) {
        case UNDERSCORE:
            languageId = languageTag.replace('-', '_');
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
