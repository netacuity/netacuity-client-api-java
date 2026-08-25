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

import java.net.InetAddress;

import net.digitalenvoy.netacuity.api.impl.DbAccessorImpl;

/**
 * A factory for obtaining instances of <code>DbAccessor</code>.
 */
public class DbAccessorFactory {

    private DbAccessorFactory() {} // inhibit construction

    /**
     * Default timeout, in milliseconds, used by {@link #getAccessor(InetAddress, int)} and
     * {@link #getAccessor(InetAddress)}.
     */
    public static final int DEFAULT_TIMEOUT_MILLIS = 2000;

    /**
     * Return an instance of DbAccessor. A new instance is created on each call.
     *
     * @param serverAddress the address of the NetAcuity Server
     * @param apiId the apiId to include in the query
     * @param timeoutMillis the time in milliseconds to wait for a response to a request
     * @return an instance of the database accessor
     */
    public static DbAccessor getAccessor(final InetAddress serverAddress, int apiId, int timeoutMillis) {
        return new DbAccessorImpl(serverAddress, apiId, timeoutMillis);
    }

    /**
     * Return an instance of DbAccessor that sends requests to a non-standard port. Real NetAcuity
     * Servers only listen on the standard port; this overload exists so tests can point the
     * accessor at an in-process mock server bound to an OS-assigned ephemeral port. A new
     * instance is created on each call.
     *
     * @param serverAddress the address of the NetAcuity Server
     * @param apiId the apiId to include in the query
     * @param timeoutMillis the time in milliseconds to wait for a response to a request
     * @param port the UDP port to send requests to
     * @return an instance of the database accessor
     */
    public static DbAccessor getAccessor(final InetAddress serverAddress, int apiId, int timeoutMillis, int port) {
        return new DbAccessorImpl(serverAddress, apiId, timeoutMillis, port);
    }

    /**
     * Return an instance of DbAccessor using the default timeout
     * ({@value #DEFAULT_TIMEOUT_MILLIS}ms). A new instance is created on each call.
     *
     * @param serverAddress the address of the NetAcuity Server
     * @param apiId the apiId to include in the query
     * @return an instance of the database accessor
     */
    public static DbAccessor getAccessor(final InetAddress serverAddress, int apiId) {
        return getAccessor(serverAddress, apiId, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Return an instance of DbAccessor using the default apiId (0) and the default timeout
     * ({@value #DEFAULT_TIMEOUT_MILLIS}ms). A new instance is created on each call.
     *
     * @param serverAddress the address of the NetAcuity Server
     * @return an instance of the database accessor
     */
    public static DbAccessor getAccessor(final InetAddress serverAddress) {
        return getAccessor(serverAddress, 0);
    }
}
