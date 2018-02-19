/*  
 * Copyright IBM Corp. 2018
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
 * The base class of resource filter exception.
 * 
 * @see IllegalResourceFormatException
 * @author yoshito_umaoka
 */
public class ResourceFilterException extends Exception {

    private static final long serialVersionUID = 1500241587685110181L;

    public ResourceFilterException() {
        super();
    }

    public ResourceFilterException(String message) {
        super(message);
    }

    public ResourceFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceFilterException(Throwable cause) {
        super(cause);
    }
}
