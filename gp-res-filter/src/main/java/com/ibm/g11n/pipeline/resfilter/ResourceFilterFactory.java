/*  
 * Copyright IBM Corp. 2015
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
package com.ibm.g11n.pipeline.resfilter;

/**
 * Factory for ResourceFilter.
 * 
 * @author Yoshito Umaoka
 */
public class ResourceFilterFactory {

    public static ResourceFilter getX(String type) {
        if (type.equalsIgnoreCase("java")) {
            return new JavaPropertiesResource();
        } else if (type.equalsIgnoreCase("json")) {
            return new JsonResource();
        } else if (type.equalsIgnoreCase("amdjs")) {
            return new AmdJsResource();
        } else if (type.equalsIgnoreCase("yml")){
            return new YMLResource();
        } else if (type.equalsIgnoreCase("xliff")) {
            return new XLIFFResource();
        } else if (type.equalsIgnoreCase("android")) {
            return new AndroidStringsResource();
        } else if (type.equalsIgnoreCase("ios")) {
            return new IOSStringsResource();
        } else if (type.equalsIgnoreCase("pot")) {
            return new POTResource();
        } else if (type.equalsIgnoreCase("po")) {
            return new POResource();
        }
        throw new IllegalArgumentException("Unknown resource type: " + type);
    }

    public static ResourceFilter get(ResourceType type) {
        switch (type) {
        case JAVA:
            return new JavaPropertiesResource();
        case JSON:
            return new JsonResource();
        case AMDJS:
            return new AmdJsResource();
        case YML:
            return new YMLResource();
        case XLIFF:
            return new XLIFFResource();
        case ANDROID:
            return new AndroidStringsResource();
        case IOS:
            return new IOSStringsResource();
        case POT:
            return new POTResource();
        case PO: 
            return new POResource();
        }
        throw new IllegalArgumentException("Unknown resource type: " + type);
    }

}
