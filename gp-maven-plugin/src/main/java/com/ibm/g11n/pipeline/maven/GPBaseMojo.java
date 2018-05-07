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
import com.ibm.g11n.pipeline.resfilter.impl.DefaultResourceFilterProvider;

/**
 * Base class of GP download/upload Mojo.
 * 
 * @author Yoshito Umaoka
 */
public abstract class GPBaseMojo extends AbstractMojo {
    public static final String LANGPARAM = "{LANG}";

    /**
     * Credentials used for accessing the instance of Globalization
     * Pipeline service. There are 4 sub-elements required: &lt;url&gt;,
     * &lt;instanceId&gt;, &lt;userId&gt; and &lt;password&gt;.
     * When &lt;credentialsJson&gt; is specified, this configuration is
     * ignored.
     */
    @Parameter
    private Credentials credentials;

    /**
     * JSON file including credentials used for accessing the instance of
     * Globalization Pipeline service. The JSON file must have string elements
     * "url", "instanceId", "userId" and "password" in the top level object.
     */
    @Parameter(defaultValue = "${gp.credentials.json}")
    private File credentialsJson;

    /**
     * Each &lt;bundleSet&gt; specifies a translation source language, a set of
     * resource bundle files in the source language, resource type and other
     * configurations.
     */
    @Parameter
    private List<BundleSet> bundleSets;

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

    protected static class SourceBundleFile {
        private String type;
        private String bundleId;
        private File file;
        private String relativePath;

        private SourceBundleFile(String type, String bundleId, File file, String relativePath) {
            this.type = type;
            this.bundleId = bundleId;
            this.file = file;
            this.relativePath = relativePath;
        }

        public String getType() {
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

    protected List<SourceBundleFile> getSourceBundleFiles(BundleSet bundleSet) {
        List<SourceBundleFile> bundleFiles = new LinkedList<SourceBundleFile>();

        FileSetManager fsm = new FileSetManager(getLog());
        File baseDir = project.getBasedir();
        String type = bundleSet.getType();
        FileSet fs = bundleSet.getSourceFiles();
        File fsBaseDir = new File(baseDir, fs.getDirectory());
        String[] relPathes = fsm.getIncludedFiles(fs);
        for (String relPath : relPathes) {
            File bundleFile = new File(fsBaseDir, relPath);
            bundleFiles.add(
                    new SourceBundleFile(type, pathToBundleId(relPath, bundleSet),
                            bundleFile, relPath));
        }
        return bundleFiles;
    }

    protected synchronized List<BundleSet> getBundleSets() {
        if (bundleSets == null) {
            // default SourceBundleSet
            FileSet fs = new FileSet();
            fs.setDirectory("src/main/resources");
            fs.addInclude("**/*.properties");
            // Note: This exclusion pattern might be too aggressive...
            fs.addExclude("**/*_*.properties");
            bundleSets = Collections.singletonList(new BundleSet(fs));
        }
        return bundleSets;
    }

    /**
     * Maps bundle file's relative path to GP bundle ID.
     * <p>
     * This method might be enhanced to support custom mappings
     * through configuration in future.
     * 
     * @param path  Relative path to package root
     * @param bundleSet BundleSet object configuration
     * @return A bundle ID corresponding to the resource type and path.
     */
    String pathToBundleId(String path, BundleSet bundleSet) {
        List<RegexMapper> ptbMappers = bundleSet.getPathToBundleMapper();
        if (ptbMappers == null) {
            return defaultPathToBundleId(bundleSet.getType(), path);
        }

        String result = path;
        for (RegexMapper mapper : ptbMappers) {
            result = mapper.map(result);
        }

        return result;
    }

    /**
     * Default implementation used for path to bundle ID mapping.
     * 
     * @param type  Resource type
     * @param path  Relative path to package root
     * @return A bundle ID corresponding to the resource type and path.
     */
    static String defaultPathToBundleId(String type, String path) {
        StringBuilder buf = new StringBuilder();
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null) {
            buf.append(parent.getPath().replace(File.separatorChar, '.').replace(' ', '_'));
        }

        char sep = '-'; // separator between package and file
        String fileName = f.getName().replace(' ', '_');
        if (DefaultResourceFilterProvider.isJavaType(type)) {
            int dotIdx = fileName.indexOf('.');
            if (dotIdx >= 0) {
                fileName = fileName.substring(0, dotIdx);
            }
            sep = '.';
        }

        if (parent != null) {
            buf.append(sep);
        }
        buf.append(fileName);

        return buf.toString();
    }

    protected Set<String> resolveTargetLanguages(BundleSet bundleSet) throws MojoFailureException {
        Set<String> targetLanguages = bundleSet.getTargetLanguages();
        if (targetLanguages == null) {
            ServiceClient client = getServiceClient();
            String srcLang = bundleSet.getSourceLanguage();
            if (srcLang == null) {
                srcLang = "en";
            }
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
