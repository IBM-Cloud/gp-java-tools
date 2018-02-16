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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * <code>MultiBundleResourceFilter</code> is an abstract class defines multiple bundle resource
 * filter interface.
 * <p>
 * A resource filter that handles multiple resource bundles through a single file such as
 * XLIFF filter may extend this class.
 * 
 * @see ResourceFilter
 * @author yoshito_umaoka
 */
public abstract class MultiBundleResourceFilter {
    /**
     * Parses the contents of resource data from the specified input stream and returns a map of
     * <code>LanguageBundle</code> with bundle identifiers as keys.
     * 
     * @param inStream  The input stream of the resource data.
     * @return  A map of <code>LanguageBundle</code> with bundle identifiers as keys.
     * @param options   The options controlling the filter's behavior. This argument is optional and
     *                  can be <code>null</code>.
     * @throws IOException  if an error occurred when reading from the input stream.
     * @throws ResourceFilterException  if an error occurred when parsing the resource contents.
     */
    public abstract Map<String, LanguageBundle> parse(InputStream inStream, FilterOptions options)
            throws IOException, ResourceFilterException;

    /**
     * Writes the map of language bundle data indexed by bundle IDs to the specified output stream in
     * a target resource format implemented by the filter class.
     * 
     * @param outStream The output steam where the resource data will be written.
     * @param languageBundles   The map of language bundle data indexed by bundle IDs.
     * @param options   The options controlling the filter's behavior. This argument is optional and
     *                  can be <code>null</code>.
     * @throws IOException  if an error occurred when writing to the output stream.
     * @throws ResourceFilterException  if an error occurred when converting language bundle data
     *                          to a target format.
     */
    public abstract void write(OutputStream outStream, Map<String, LanguageBundle> languageBundles,
            FilterOptions options) throws IOException, ResourceFilterException;

    /**
     * Merges the map of language bundle data indexed by bundle IDs into the corresponding multiple bundle
     * resource. A concrete subclass of this class may override this method if such operation is available.
     * The default implementation calls {@link #write(OutputStream, Map, FilterOptions)}.
     * 
     * @param baseStream    The input stream of base multiple bundle resource.
     * @param outStream The output stream where the resource data will be written.
     * @param languageBundles   The map of language bundle data indexed by bundle IDs.
     * @param options   The options controlling the filter's behavior. This argument is optional and
     *                  can be <code>null</code>.
     * @throws IOException  if an error occurred when reading form the input stream, or writing to
     *                      the output stream.
     * @throws ResourceFilterException  if an error occurred when converting language bundle data
     *                          to a target format.
     */
    public void merge(InputStream baseStream, OutputStream outStream, Map<String, LanguageBundle> languageBundles,
            FilterOptions options) throws IOException, ResourceFilterException {
        write(outStream, languageBundles, options);
    }
}
