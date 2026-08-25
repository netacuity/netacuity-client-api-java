/*
 * Copyright 2026 Digital Envoy, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.digitalenvoy.netacuity.api;

import java.util.Map;

/**
 * A response returned when the DbAccessor is used to perform an XML based query to NetAcuity.
 */
public interface XmlResponse {

    /**
     * Return a <code>Map</code> containing the key/value pairs contained in the database response
     *
     * @return the key/value mappings returned by the server
     */
    public Map<String, String> getResponseMap();

    /**
     * Returns the unprocessed raw response that was received from the NetAcuity server.
     *
     * @return the unmodified XML response string
     */
    public String getRawResponse();
}
