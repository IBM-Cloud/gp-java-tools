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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.ResourceType;

/**
 * Base class of GP import/export Mojo.
 * 
 * @author Yoshito Umaoka
 */
public abstract class GPBaseMojo extends AbstractMojo {
    @Parameter
    private Credentials credentials;

    @Parameter(defaultValue = "${gp.credentials.json}")
    private File credentialsJson;

    @Parameter
    private List<SourceBundleSet> sourceBundleSets;

    @Parameter
    private String sourceLanguage;

    @Parameter
    private Set<String> targetLanguages;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private volatile ServiceClient gpClient = null;

    /**
     * Returns GP ServiceClient instance used for this for
     * this maven session.
     * <p>
     * This implementation resolves service credentials in
     * the following order:
     * <ol>
     *   <li>A json file specified by user defined property 'gp.credentials.json'.
     *   This is typically specified by a command line argument, e.g.<br>
     *   <code>-D gp.credentials.json=/home/yoshito/gpcreds.json</code></li>
     *   <li>A json file specified by &lt;credentialsJson&gt; in pom.xml</li>
     *   <li>A set of fields specified by &lt;credentials&gt; in pom.xml</li>
     * </ol>
     * @return An instance of ServiceClient.
     * @throws MojoFailureException on a failure.
     */
    protected ServiceClient getServiceClient() throws MojoFailureException {
        if (gpClient == null) {
            synchronized (this) {
                Credentials creds = credentials;
                if (credentialsJson != null) {
                    getLog().info("Reading GP service credentials from " + credentialsJson.getAbsolutePath());
                    try (InputStreamReader reader = new InputStreamReader(
                            new FileInputStream(credentialsJson), StandardCharsets.UTF_8)) {
                        Gson gson = new Gson();
                        creds = gson.fromJson(reader, Credentials.class);
                    } catch (IOException e) {
                        throw new MojoFailureException("Error while reading the specified JSON credential file.", e);
                    } catch (JsonSyntaxException e) {
                        throw new MojoFailureException("Bad JSON credential format.", e);
                    }
                }

                if (creds == null) {
                    throw new MojoFailureException(
                            "Globalization Pipeline service credentials are not specified.");
                }

                if (!creds.isValid()) {
                    throw new MojoFailureException("Bad credentials: " + creds);
                }

                getLog().debug("Using GP service credentials " + creds);

                gpClient = ServiceClient.getInstance(
                        ServiceAccount.getInstance(
                                creds.getUrl(), creds.getInstanceId(),
                                creds.getUserId(), creds.getPassword()));
            }
        }
        return gpClient;
    }

    protected static class BundleFile {
        private ResourceType type;
        private String bundleId;
        private File file;
        private String relativePath;

        private BundleFile(ResourceType type, String bundleId, File file, String relativePath) {
            this.type = type;
            this.bundleId = bundleId;
            this.file = file;
            this.relativePath = relativePath;
        }

        public ResourceType getType() {
            return type;
        }

        public String getBundleId() {
            return bundleId;
        }

        public File getFile() {
            return file;
        }

        public String getRelativePath() {
            return relativePath;
        }
    }

    protected List<BundleFile> getSourceBundleFiles() {
        List<BundleFile> bundleFiles = new LinkedList<BundleFile>();

        List<SourceBundleSet> bundleSets = getSourceBundleSets();
        FileSetManager fsm = new FileSetManager(getLog());
        File baseDir = project.getBasedir();
        for (SourceBundleSet bset : bundleSets) {
            ResourceType type = bset.getType();
            FileSet fs = bset.getFiles();
            File fsBaseDir = new File(baseDir, fs.getDirectory());
            String[] relPathes = fsm.getIncludedFiles(fs);
            for (String relPath : relPathes) {
                File bundleFile = new File(fsBaseDir, relPath);
                bundleFiles.add(
                        new BundleFile(type, pathToBundleId(type, relPath),
                                bundleFile, relPath));
            }
        }
        return bundleFiles;
    }

    private synchronized List<SourceBundleSet> getSourceBundleSets() {
        if (sourceBundleSets == null) {
            // default SourceBundleSet
            FileSet fs = new FileSet();
            fs.setDirectory("src/main/resources");
            fs.addInclude("**/*.properties");
            sourceBundleSets = Collections.singletonList(
                    new SourceBundleSet(ResourceType.JAVA, fs));
        }
        return sourceBundleSets;
    }

    /**
     * Maps bundle file's relative path to GP bundle ID.
     * <p>
     * This method might be enhanced to support custom mappings
     * through configuration in future.
     * 
     * @param type  Resource type
     * @param path  Relative path to package root
     * @return A bundle ID corresponding to the resource type and path.
     */
    private String pathToBundleId(ResourceType type, String path) {
        File f = new File(path);
        File parent = f.getParentFile();
        String pkgName = parent == null ? "" :
            parent.getPath().replace(File.separatorChar, '.');

        String fileName = f.getName();
        if (type == ResourceType.JAVA) {
            int dotIdx = fileName.indexOf('.');
            if (dotIdx >= 0) {
                fileName = fileName.substring(0, dotIdx);
            }
            return pkgName + "." + fileName;
        }

        return pkgName + "-" + fileName;
    }

    protected synchronized String getSourceLanguage() {
        if (sourceLanguage == null || sourceLanguage.isEmpty()) {
            getLog().info("The configuration parameter 'sourceLanguage' is not specified."
                    + " Using the defualt language English (en)");
            sourceLanguage = "en";
        }
        return sourceLanguage;
    }

    protected synchronized Set<String> getTargetLanguages() throws MojoFailureException {
        if (targetLanguages == null) {
            ServiceClient client = getServiceClient();
            String srcLang = getSourceLanguage();

            // targetLanguages is not specified. Default to all available languages.
            try {
                Map<String, Set<String>> activeMTLangs = client.getConfiguredMTLanguages();
                targetLanguages = activeMTLangs.get(srcLang);
            } catch (ServiceException e) {
                targetLanguages = Collections.emptySet();
                throw new MojoFailureException("Globalization Pipeline service error", e);
            }

            getLog().info("The configuration parameter 'targetLanguages' is not specified."
                    + " Using currently active target languages: " + targetLanguages);
        }
        return Collections.unmodifiableSet(targetLanguages);
    }
}
