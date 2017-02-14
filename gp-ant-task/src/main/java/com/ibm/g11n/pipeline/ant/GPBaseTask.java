/*  
 * Copyright IBM Corp. 2017
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ibm.g11n.pipeline.client.ServiceAccount;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;
import com.ibm.g11n.pipeline.resfilter.ResourceType;

/**
 * Base class of GP download/upload ant tasks
 * 
 * @author Yoshito Umaoka
 * @author Jugu
 */
public abstract class GPBaseTask extends Task{
    /**
     * Credentials used for accessing the instance of Globalization
     * Pipeline service. There are 4 sub-elements required: &lt;url&gt;,
     * &lt;instanceId&gt;, &lt;userId&gt; and &lt;password&gt;.
     * When &lt;credentialsJson&gt; is specified, this configuration is
     * ignored.
     */
    protected Credentials credentials;

    public void addCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * JSON file including credentials used for accessing the instance of
     * Globalization Pipeline service. The JSON file must have string elements
     * "url", "instanceId", "userId" and "password" in the top level object.
     */

    private File credentialsJson;

    public void setCredentialsJson(File credjson) {
        this.credentialsJson = credjson;
    }
    
    /**
     * Source directory of the translation files.(Mandatory)
     */
    private File sourceDir;
    
    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir;
    }
    
    /**
     * Each &lt;bundleSet&gt; specifies a translation source language, a set of
     * resource bundle files in the source language, resource type and other
     * configurations.
     */
    private List<BundleSet> bundleSets= new ArrayList<BundleSet>();

    private volatile ServiceClient gpClient = null;

    /**
     * Set of targetlanugages defined as implicit default
     */
    protected Set<TargetLanguage> targetLanguages = new HashSet<TargetLanguage>();

    public void addTargetLanguage(TargetLanguage tl) {
        targetLanguages.add(tl);
    }


    public void execute() {
    }

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
     * @throws BuildExcpetion on a failure.
     */
    protected ServiceClient getServiceClient() {
        if (gpClient == null) {
            synchronized (this) {
                Credentials creds = credentials;
                if (credentialsJson != null) {
                    getProject().log("Reading GP service credentials from " + credentialsJson.getAbsolutePath(), Project.MSG_INFO);
                    try (InputStreamReader reader = new InputStreamReader(
                            new FileInputStream(credentialsJson), StandardCharsets.UTF_8)) {
                        Gson gson = new Gson();
                        creds = gson.fromJson(reader, Credentials.class);
                    } catch (IOException e) {
                        throw new BuildException("Error while reading the specified JSON credential file.", e);
                    } catch (JsonSyntaxException e) {
                        throw new BuildException("Bad JSON credential format.", e);
                    }
                }

                if (creds == null) {
                    throw new BuildException(
                            "Globalization Pipeline service credentials are not specified.");
                }

                if (!creds.isValid()) {
                    throw new BuildException("Bad credentials: " + creds);
                }

                getProject().log("Using GP service credentials " + creds, Project.MSG_DEBUG);
                gpClient = ServiceClient.getInstance(
                        ServiceAccount.getInstance(
                                creds.getUrl(), creds.getInstanceId(),
                                creds.getUserId(), creds.getPassword()));
            }
        }
        return gpClient;
    }

    protected static class SourceBundleFile {
        private ResourceType type;
        private String bundleId;
        private File file;
        private String relativePath;

        private SourceBundleFile(ResourceType type, String bundleId, File file, String relativePath) {
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

    protected List<SourceBundleFile> getSourceBundleFiles(BundleSet bundleSet) {
        List<SourceBundleFile> bundleFiles = new LinkedList<SourceBundleFile>();

        ResourceType type = bundleSet.getType();
        FileSet fs = bundleSet.getSourceFiles();
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();
        File base  = ds.getBasedir();
        for (int i = 0; i < includedFiles.length; i++) {
            File bundleFile = new File(base, includedFiles[i]);
            bundleFiles.add(
                    new SourceBundleFile(type, pathToBundleId(type, includedFiles[i]),
                            bundleFile, includedFiles[i]));
        }
        return bundleFiles;
    }

    protected synchronized List<BundleSet> getBundleSets() throws FileNotFoundException {
        if (bundleSets.isEmpty()) {
            // default SourceBundleSet
            FileSet fs = new FileSet();
            fs.setDir(sourceDir);
            fs.setIncludes("**/*.properties");
            // Note: This exclusion pattern might be too aggressive...
            fs.setExcludes("**/*_*.properties");
            System.out.println("bundleset processing");
            bundleSets = Collections.singletonList(new BundleSet(fs));
        }
        return bundleSets;
    }

    public BundleSet createBundleSet() {
        BundleSet bundleSet = new BundleSet();
        bundleSets.add(bundleSet);
        return bundleSet;
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

    protected Set<String> resolveTargetLanguages(BundleSet bundleSet) throws BuildException {
        Set<String> targetLanguages = bundleSet.getTargetLanguages();
        String srcLang = bundleSet.getSourceLanguage();
        if (srcLang == null) {
            srcLang = "en";
        }
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            targetLanguages = new HashSet<String>();
            for (TargetLanguage tl : this.targetLanguages) {
                if (srcLang.equals(tl.getLang()))
                    continue;
                targetLanguages.add(tl.getLang());
            }
        }
        if (targetLanguages.isEmpty()) {
            ServiceClient client = getServiceClient();
            // targetLanguages is not specified. Default to all available languages.
            try {
                Map<String, Set<String>> activeMTLangs = client.getConfiguredMTLanguages();
                targetLanguages = activeMTLangs.get(srcLang);
            } catch (ServiceException e) {
                targetLanguages = Collections.emptySet();
                throw new BuildException("Globalization Pipeline service error", e);
            }

            getProject().log("The configuration parameter 'targetLanguages' is not specified."
                    + " Using currently active target languages: " + targetLanguages, Project.MSG_INFO);
        }
        return Collections.unmodifiableSet(targetLanguages);
    }  
}
