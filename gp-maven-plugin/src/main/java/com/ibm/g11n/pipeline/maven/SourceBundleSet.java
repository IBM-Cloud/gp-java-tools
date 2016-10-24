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

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;

import com.ibm.g11n.pipeline.resfilter.ResourceType;

/**
 * SourceBundleSet class is used for receiving the set of
 * resource bundle files and types from maven plugin configuration.
 * 
 * @author Yoshito Umaoka
 */
class SourceBundleSet {
    @Parameter(defaultValue = "JAVA", required = true)
    private ResourceType type;

    @Parameter(required = true)
    private FileSet files;

    SourceBundleSet() {
    }

    SourceBundleSet(ResourceType type, FileSet files) {
        this.type = type;
        this.files = files;
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
     * @return the files
     */
    public FileSet getFiles() {
        return files;
    }

    /**
     * @param files the files to set
     */
    public void setFiles(FileSet files) {
        this.files = files;
    }
}
