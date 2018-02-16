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
 * An exception type used for illegal resource format.
 * 
 * @author yoshito_umaoka
 */
public class IllegalResourceFormatException extends ResourceFilterException {

    private static final long serialVersionUID = -940881304674153669L;

    public IllegalResourceFormatException() {
        super();
    }

    public IllegalResourceFormatException(String message) {
        super(message);
    }

    public IllegalResourceFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalResourceFormatException(Throwable cause) {
        super(cause);
    }
}
