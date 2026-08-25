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

package net.digitalenvoy.netacuity.api.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.digitalenvoy.netacuity.api.DbAccessor;
import net.digitalenvoy.netacuity.api.NetAcuityException;
import net.digitalenvoy.netacuity.api.NetAcuityFeature;
import net.digitalenvoy.netacuity.api.XmlResponse;
import net.digitalenvoy.netacuity.api.util.IpAddressValidator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Concrete implementation of DbAccessor that can access a NetAcuity database service over a UDP connection.
 * A socket pool, shared by all instances of this class, is included to reuse open datagram sockets
 * when communicating with the NetAcuity Version 5 (and above) protocol,
 */
public class DbAccessorImpl implements DbAccessor {

    private final static Charset UTF_8 = StandardCharsets.UTF_8;
    private final static int PACKET_SIZE = 1500; // datagram buffer size
    private final static String QUOTE = "\"";
    public final static int DEFAULT_PORT = 5400;

    private static final AtomicInteger threadCount = new AtomicInteger(0);

    private StatsManager sm = null;

    private final InetAddress serverAddress;
    private final static SocketPool socketPool = new SocketPool();
    private final static SecureRandom transactionIdRandom = new SecureRandom();

    private final int timeoutMillis;
    private final int apiID;
    private final int port;

    // A sequential counter would let an attacker predict/guess the transaction ID and
    // forge a response that passes the echo-validation check below.
    private static long generateTransactionId() {
        return transactionIdRandom.nextLong() & Long.MAX_VALUE;
    }

    // A socket pool with an unbounded cache. The number of entries
    // should never exceed the maximum number of threads.
    private static class SocketPool {

        // use a linked list since access is limited to insertion/removal of nodes,
        // which linked lists are optimized for
        LinkedList<DatagramSocket> sockets = new LinkedList<>();

        DatagramSocket getSocket(int timeoutMillis) throws SocketException {
            while (true) {
                DatagramSocket so = null;
                synchronized (sockets) {
                    so = sockets.pollFirst();
                }
                // make sure the socket is not closed. I don't think that can happen
                // at this point, but the JavaDoc is vague, so acting defensively here
                if (so != null) {
                    if (so.isClosed()) {
                        continue; // discard closed sockets, though
                    }
                } else {
                    so = new DatagramSocket();
                }
                so.setSoTimeout(timeoutMillis);
                return so;
            }
        }

        void returnSocket(DatagramSocket so) {
            if (! so.isClosed()) {  // exception handlers may have already closed the socket
                synchronized (sockets) {
                    sockets.add(so);
                }
            }
        }
    }

    /**
     * Create a UDP database client that queries the NetAcuity Server on its standard port.
     *
     * @param serverAddress the address of the NetAcuity Server
     * @param apiId the apiId to include in calls to the server
     * @param timeoutMillis the time in milliseconds to wait before aborting the call to the server
     */
    public DbAccessorImpl(final InetAddress serverAddress, int apiId, int timeoutMillis) {
        this(serverAddress, apiId, timeoutMillis, DEFAULT_PORT);
    }

    /**
     * Create a UDP database client targeting a non-standard port. Real NetAcuity Servers only
     * listen on {@code DEFAULT_PORT} ({@value #DEFAULT_PORT}); this overload exists so tests can
     * point the client at an in-process mock server bound to an OS-assigned ephemeral port,
     * avoiding the flakiness of every test run competing for the same fixed port.
     *
     * @param serverAddress the address of the NetAcuity Server
     * @param apiId the apiId to include in calls to the server
     * @param timeoutMillis the time in milliseconds to wait before aborting the call to the server
     * @param port the UDP port to send requests to
     */
    public DbAccessorImpl(final InetAddress serverAddress, int apiId, int timeoutMillis, int port) {
        if (apiId < 0 || apiId > 127) {
            throw new IllegalArgumentException("invalid apiId: " + apiId);
        }
        this.serverAddress = serverAddress;
        this.apiID = apiId;
        this.timeoutMillis = timeoutMillis;
        this.port = port;
    }

    // Seam allowing tests to simulate a socket-creation failure (e.g. SocketException) without
    // needing a real OS-level failure condition; the shared, static socketPool itself is not
    // altered, so overriding this in a test subclass does not affect other DbAccessorImpl instances.
    protected DatagramSocket acquireSocket() throws SocketException {
        return socketPool.getSocket(timeoutMillis);
    }

    // inherit JavaDoc
    @Override
    public XmlResponse queryXml(List<NetAcuityFeature> features, InetAddress queryAddress)
            throws IOException, NetAcuityException
    {
        List<Integer> featuresCodes = new ArrayList<>();
        for (NetAcuityFeature feature : features) {
            featuresCodes.add(feature.getFeatureCode());
        }
        String transactionId = Long.toString(generateTransactionId());
        return queryXml(featuresCodes, queryAddress, transactionId);
    }

    @Override
    public XmlResponse queryXml(List<Integer> featureCodes, InetAddress queryAddress, String transactionId)
            throws IOException, NetAcuityException
    {
        // transactionId is embedded unescaped into an XML attribute below (see quoted()); a value
        // containing one of these characters would break out of the attribute and inject
        // arbitrary XML into the request.
        if (transactionId != null && containsXmlSpecialCharacter(transactionId)) {
            throw new IllegalArgumentException("invalid transactionId: contains disallowed character");
        }
        StatsManager.StatsItem item = null;
        synchronized (this) {
            if (sm != null) {
                item = new StatsManager.StatsItem();
            }
        }
        if (item != null) {
            item.entryTime = System.currentTimeMillis();
        }
        final Map<String, String> resultMap = new LinkedHashMap<>();
        String response = null; // captured before validation runs, so it can be attached to a thrown NetAcuityException
        DatagramSocket socket = acquireSocket();
        // Restricts the OS to only deliver datagrams from serverAddress on this socket,
        // rejecting spoofed/stray packets from any other source before they reach this code.
        socket.connect(serverAddress, port);
        if (item != null) {
            item.socketAcquisitionTime = System.currentTimeMillis();
        }
        try {
            StringBuilder queryBuilder =
                new StringBuilder("<request")
                          .append(    " trans-id=").append(quoted(transactionId))
                          .append(    " ip=")      .append(quoted(queryAddress.getHostAddress()))
                          .append(    " api-id=")  .append(quoted(apiID))
                          .append(" >");
            for (int featureCode : featureCodes) {
              if (featureCode < 3 || featureCode >= 100) {
                  throw new IllegalArgumentException("invalid feature code: " + featureCode);
              }
              queryBuilder.append("<query db=").append(quoted(featureCode)).append("/>");
            }
            queryBuilder.append("</request>");
            byte[] requestBuf = queryBuilder.toString().getBytes(UTF_8);
            DatagramPacket queryPacket = new DatagramPacket(requestBuf, requestBuf.length, serverAddress, port);
            int threadCountBeforeCall = threadCount.getAndIncrement();
            if (item != null) {
                item.requestTime = System.currentTimeMillis();
                item.callsInProgress = threadCountBeforeCall;
                item.timedOut = true; // assume timeout, fix later if not, since not catching timeout exceptions
            }
            socket.send(queryPacket);
            int expectedPacket = 1;
            byte[] responseBuf = new byte[PACKET_SIZE];
            StringBuilder responseBuilder = new StringBuilder();
            while (true) {
                DatagramPacket responsePacket = new DatagramPacket(responseBuf , responseBuf.length);
                socket.receive(responsePacket);
                if (item != null) {
                    item.responseTime = System.currentTimeMillis();
                    item.timedOut = false;
                }
                String responseString = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength(), UTF_8);
                // discard trailing nulls
                int nullIndex = responseString.indexOf("\0");
                if (nullIndex >= 0) {
                    responseString = responseString.substring(0, nullIndex);
                }
                response = responseBuilder.toString() + responseString; // best-effort snapshot in case this packet fails validation below
                if (responseString.length() < 4) {
                    throw new NetAcuityException("invalid response packet: " + responseString);
                }
                int packetNumber = Integer.parseInt(responseString.substring(0,2));
                if (packetNumber != expectedPacket++) {
                    throw new NetAcuityException("packets received out of order");
                }
                responseBuilder.append(responseString.substring(4));
                response = responseBuilder.toString();
                int packetTotal = Integer.parseInt(responseString.substring(2,4));
                if (item != null) {
                    item.responsePacketCount = packetTotal;
                }
                if (packetNumber == packetTotal) {
                    break;
                }
            }
            // populate the map with the response attributes
            try (InputStream inputStream = new ByteArrayInputStream(response.getBytes(UTF_8))) {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                DefaultHandler handler = new DefaultHandler() {
                    @Override
                    public void startElement(String uri,
                                             String localName,
                                             String qName,
                                             Attributes attributes) throws SAXException {
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String fieldName = attributes.getQName(i);
                            String fieldValue = attributes.getValue(i);
                            resultMap.put(fieldName, fieldValue);
                        }
                    }
                };
                parser.parse(inputStream, handler);
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException("failed to parse the XML response", e);
            }

            // make absolutely sure the response is in sync with the request. If not simulate a timeout
            // so upper layers can retry if desired
            String returnedAddress = resultMap.get("ip");
            String returnedTransactionId = resultMap.get("trans-id");
            if (returnedTransactionId == null || !returnedTransactionId.equals(transactionId)) {
                throw new NetAcuityException("response transactionID is out of sync with request transactionID");
            }
            if (!IpAddressValidator.isValid(returnedAddress)) {
                throw new NetAcuityException("response address is not a valid IP address literal");
            }
            InetAddress returnedInetAddress;
            try {
                returnedInetAddress = InetAddress.getByName(returnedAddress);
            } catch (UnknownHostException e) {
                throw new NetAcuityException("response address is out of sync with request address");
            }
            // Compare as InetAddress objects, not strings: the server may return an
            // IPv6 address in a differently-compressed (but equal) textual form.
            if (!queryAddress.equals(returnedInetAddress)) {
                throw new NetAcuityException("response address is out of sync with request address");
            }
            String returnedError = resultMap.get("error");
            if (returnedError != null && !returnedError.isEmpty()) {
                throw new NetAcuityException(returnedError);
            }
            return new XmlResponseImpl(resultMap, response);
        } catch (NetAcuityException e) {
            if (response != null) {
                e.setRawResponse(response);
            }
            socket.close();
            throw e;
        } catch (IOException e) {
            socket.close();
            throw e;
        } finally {
            threadCount.decrementAndGet();
            socketPool.returnSocket(socket);
            if (item != null) {
                item.returnTime = System.currentTimeMillis();
                synchronized (this) {
                    if (sm != null) { // could have changed during the call
                        sm.updateStats(item);
                    }
                }
            }
        }
    }

    private String quoted(String s) {
        return QUOTE + (s == null ? "" : s) + QUOTE;
    }

    private static boolean containsXmlSpecialCharacter(String s) {
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '"':
                case '<':
                case '>':
                case '&':
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    private String quoted(int i) {
        return QUOTE + i + QUOTE;
    }

    @Override
    public void startStatsManager() {
        synchronized (this) {
            sm = new StatsManager();
        }
    }

    @Override
    public void stopStatsManager() {
        synchronized (this) {
            sm = null;
        }
    }

    @Override
    public String dumpStats() {
        synchronized (this) {
            if (sm != null) {
                return sm.dumpStats();
            } else {
                return "";
            }
        }
    }

    @Override
    public void resetStats() {
        synchronized (this) {
            if (sm != null) {
                sm.reset();
            }
        }
    }

    // round-trip and total call histograms 0-10ms 10-50ms 50-200ms 200-1000ms >1000ms
    // socket histo: 0-2ms 2-5ms 5-10ms >10ms
    private static class StatsManager {

        int socketAcquisitionMin;
        int socketAcquisitionMax;
        int sock0to2;
        int sock2to5;
        int sock5to10;
        int sock10plus;

        int roundTripMin;
        int roundTripMax;
        int rt0to10;
        int rt10to50;
        int rt50to200;
        int rt200to1k;
        int rt1kplus;

        int totalCallTimeMin;
        int totalCallTimeMax;
        int tc0to10;
        int tc10to50;
        int tc50to200;
        int tc200to1k;
        int tc1kplus;

        int responsePacketCountMax;

        int callsInProgressMax;

        int totalCalls;
        int timeoutTotal;

        StatsManager() {
            reset();
        }

        void reset() {
            synchronized (this) {
                socketAcquisitionMin = Integer.MAX_VALUE;
                socketAcquisitionMax = 0;
                sock0to2 = 0;
                sock2to5 = 0;
                sock5to10 = 0;
                sock10plus = 0;

                roundTripMin = Integer.MAX_VALUE;
                roundTripMax = 0;
                rt0to10 = 0;
                rt10to50 = 0;
                rt50to200 = 0;
                rt200to1k = 0;
                rt1kplus = 0;

                totalCallTimeMin = Integer.MAX_VALUE;
                totalCallTimeMax = 0;
                tc0to10 = 0;
                tc10to50 = 0;
                tc50to200 = 0;
                tc200to1k = 0;
                tc1kplus = 0;

                responsePacketCountMax = 0;

                callsInProgressMax = 0;

                totalCalls = 0;
                timeoutTotal = 0;

            }
        }

        void updateStats(StatsItem item) {
            synchronized (this) {

                totalCalls++;

                int sa = (int) (item.socketAcquisitionTime - item.entryTime);
                socketAcquisitionMin = sa < socketAcquisitionMin ? sa : socketAcquisitionMin;
                socketAcquisitionMax = sa > socketAcquisitionMax ? sa : socketAcquisitionMax;
                if (sa <= 2) {
                    sock0to2++;
                } else if (sa <= 5) {
                    sock2to5++;
                } else if (sa <= 10) {
                    sock5to10++;
                } else {
                    sock10plus++;
                }

                if (!item.timedOut) {
                    int rt = (int) (item.responseTime - item.requestTime);
                    roundTripMin = rt < roundTripMin ? rt : roundTripMin;
                    roundTripMax = rt > roundTripMax ? rt : roundTripMax;
                    if (rt <= 10) {
                        rt0to10++;
                    } else if (rt <= 50) {
                        rt10to50++;
                    } else if (rt <= 200) {
                        rt50to200++;
                    } else if (rt <= 1000) {
                        rt200to1k++;
                    } else {
                        rt1kplus++;
                    }
                }

                int tc = (int) (item.returnTime - item.entryTime);
                totalCallTimeMin = tc < totalCallTimeMin ? tc : totalCallTimeMin;
                totalCallTimeMax = tc > totalCallTimeMax ? tc : totalCallTimeMax;
                if (tc <= 10) {
                    tc0to10++;
                } else if (tc <= 50) {
                    tc10to50++;
                } else if (tc <= 200) {
                    tc50to200++;
                } else if (tc <= 1000) {
                    tc200to1k++;
                } else {
                    tc1kplus++;
                }

                responsePacketCountMax = item.responsePacketCount > responsePacketCountMax ? item.responsePacketCount : responsePacketCountMax;

                callsInProgressMax = item.callsInProgress > callsInProgressMax ? item.callsInProgress : callsInProgressMax;
                if (item.timedOut) {
                    timeoutTotal++;
                }
            }
        }

        String dumpStats() {
            StringBuilder b = new StringBuilder();
            synchronized (this) {
                b.append("total attempted na server calls: ").append(totalCalls).append("\n");
                b.append("total na server timeouts: ").append(timeoutTotal).append("\n");
                b.append("min socket acquisition time: ").append(socketAcquisitionMin).append("\n");
                b.append("max socket acquisition time: ").append(socketAcquisitionMax).append("\n");
                b.append("socket acq histo: 0-2ms=").append(sock0to2).append(" 2-5ms=").append(sock2to5).append(" 5-10ms=").append(sock5to10).append(" >10ms=").append(sock10plus).append("\n");
                b.append("\n");
                b.append("min na round-trip time: ").append(roundTripMin).append("\n");
                b.append("max na round-trip time: ").append(roundTripMax).append("\n");
                b.append("round-trip histo: 0-10ms=").append(rt0to10).append(" 10-50ms=").append(rt10to50).append(" 50-200ms=").append(rt50to200).append(" 200-1000ms=").append(rt200to1k).append(" >1000ms=").append(rt1kplus).append("\n");
                b.append("\n");
                b.append("min na total call time: ").append(totalCallTimeMin).append("\n");
                b.append("max na total call time: ").append(totalCallTimeMax).append("\n");
                b.append("na total call histo: 0-10ms=").append(tc0to10).append(" 10-50ms=").append(tc10to50).append(" 50-200ms=").append(tc50to200).append(" 200-1000ms=").append(tc200to1k).append(" >1000ms=").append(tc1kplus).append("\n");
                b.append("\n");
                b.append("max response packets seen for a single call: ").append(responsePacketCountMax).append("\n");
                b.append("max simultaneous datagram requests: ").append(callsInProgressMax).append("\n");
            }
            return b.toString();
        }


        private static class StatsItem {
            long entryTime; // time method was entered
            long socketAcquisitionTime; // time socket was acquired
            long requestTime; // time immediately before request
            long responseTime; // time immediately after last response
            long returnTime; // time immediately before method return
            int callsInProgress; // number of netAcuity calls in progress, not including this one
            int responsePacketCount; // number of packets in response
            boolean timedOut = false;
        }
    }
}
