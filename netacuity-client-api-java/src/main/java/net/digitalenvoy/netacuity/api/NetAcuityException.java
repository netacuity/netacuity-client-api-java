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

import java.io.IOException;

/**
 * An exception that is thrown when the response to a NetAcuity Server query contains an error
 * message or is invalid in some way. A response is invalid if the response headers are
 * inconsistent with the information sent in the original request, or if the fields in the response data
 * do not match the expected fields in number or type.
 */
public class NetAcuityException extends IOException {

    private final static long serialVersionUID = 1L;

    private String rawResponse;

    public NetAcuityException(String message) {
        super(message);
    }

    public NetAcuityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the raw response text that was received from the server before this failure
     * was detected, or {@code null} if no response bytes had been received yet.
     *
     * @return the raw response, or {@code null}
     */
    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
