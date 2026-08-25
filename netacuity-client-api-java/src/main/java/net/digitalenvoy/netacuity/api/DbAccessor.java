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
import java.net.InetAddress;
import java.util.List;

/**
 * A Communications endpoint that performs queries against a NetAcuity database server.
 */
public interface DbAccessor {

    /**
     * Queries the given queryAddress against the NetAcuity databases represented by the
     * list of features.
     * <p>
     * The underlying XML-based UDP protocol is designed to support querying multiple databases
     * using a single network request. The response from the database is an XML element of the
     * form:
     * <pre>
     *    &lt;response name1="value1" name2="value2" ... /&gt;
     * </pre>
     * where <code>nameX</code> and <code>valueX</code> are database dependent field names
     * and their corresponding values.
     * <p>
     * For example, the following code fragment obtains the values from the Pulse, VPN Proxy,
     * and IPC databases for <code>queryAddress</code>:
     * <pre>
     *     List&lt;NetAcuityFeature&gt; features = Arrays.asList(NetAcuityFeature.PULSE, NetAcuityFeature.VPN_PROXY, NetAcuityFeature.IPC);
     *     Map&lt;String, String&gt; map = dbAccessor.queryXml(features, queryAddress);
     * </pre>
     * where <code>dbAccessor</code> is an instance of <code>DbAccessor</code>
     * previously obtained from <code>DbAccessorFactory</code>.
     *
     * @param features the databases to query
     * @param queryAddress the address to query about
     * @return an XmlResponse instance containing the response value mappings and the raw response
     *
     * @throws IOException if a communications error occurs
     * @throws SocketException if there is an error opening the datagram socket
     * @throws SocketTimeoutException if a response is not received within the timeout period
     * @throws NetAcuityException if the server responded with an error message or an invalid response
     */
    public XmlResponse queryXml(List<NetAcuityFeature> features, InetAddress queryAddress)
        throws IOException, NetAcuityException;

    /**
     * Queries the given queryAddress against the NetAcuity databases represented by the
     * list of integer feature codes.
     * <p>
     * The underlying XML-based UDP protocol is designed to support querying multiple databases
     * using a single network request. The response from the database is an XML element of the
     * form:
     * <pre>
     *    &lt;response name1="value1" name2="value2" ... /&gt;
     * </pre>
     * where <code>nameX</code> and <code>valueX</code> are database dependent field names
     * and their corresponding values.
     * <p>
     *
     * @param featureCodes the databases to query
     * @param queryAddress the address to query about
     * @param transactionId a caller-supplied transaction ID that is passed in the remote call to the
     *        NetAcuity server and is verified against the value echoed back in the response
     *
     * @return an XmlResponse instance containing the response value mappings and the raw response
     *
     * @throws IOException if a communications error occurs
     * @throws SocketException if there is an error opening the datagram socket
     * @throws SocketTimeoutException if a response is not received within the timeout period
     * @throws NetAcuityException if the server responded with an error message or an invalid response
     * @throws IllegalArgumentException if <code>transactionId</code> contains a character
     *         (<code>"</code>, <code>&lt;</code>, <code>&gt;</code>, or <code>&amp;</code>) that
     *         would break out of its XML attribute in the request sent to the server
     */
    public XmlResponse queryXml(List<Integer> featureCodes, InetAddress queryAddress, String transactionId)
        throws IOException, NetAcuityException;

    /**
     * Begin collecting round-trip and socket-acquisition statistics for calls made through this accessor.
     */
    void startStatsManager();

    /**
     * Stop collecting statistics and discard any counters collected so far.
     */
    void stopStatsManager();

    /**
     * Returns a human-readable dump of the statistics collected since the stats manager was started
     * (or last reset), or an empty string if the stats manager is not running.
     *
     * @return the statistics dump
     */
    String dumpStats();

    /**
     * Reset the statistics counters collected so far. Has no effect if the stats manager is not running.
     */
    void resetStats();

}
