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
package com.ibm.g11n.pipeline.tools.cli;

import java.util.Locale;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.ibm.g11n.pipeline.resfilter.ResourceType;

/**
 * Convert resource type parameter String to ResourceType.
 * 
 * @author Yoshito Umaoka
 */
public class ResourceTypeConverter implements IStringConverter<ResourceType>{

    @Override
    public ResourceType convert(String type) {
        try {
            return ResourceType.valueOf(type.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Parameter value " + type +
                    " is not valid for resource type option.");
        }
    }
}
