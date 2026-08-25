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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises {@code DbAccessorImpl}'s XML UDP wire protocol handling end-to-end against an
 * in-process {@link FakeNetAcuityServer}, covering error responses, multi-packet reassembly,
 * and timeouts.
 */
public class DbAccessorIntegrationTest {

    @Test
    public void query_xmlProtocol_singlePacketResponse_populatesResponseMap() throws Exception {
        String queryIp = "192.0.2.2";
        String transactionId = "test-txn-1";
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0101<response ip=\"" + queryIp + "\" trans-id=\"" + transactionId
                        + "\" country=\"usa\" city=\"atlanta\"/>").getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            XmlResponse response = accessor.queryXml(Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), transactionId);

            assertEquals("usa", response.getResponseMap().get("country"));
            assertEquals("atlanta", response.getResponseMap().get("city"));
            assertEquals(queryIp, response.getResponseMap().get("ip"));
        }
    }

    @Test
    public void query_xmlProtocol_multiPacketResponse_reassemblesAcrossPackets() throws Exception {
        String queryIp = "192.0.2.3";
        String transactionId = "multi-1";
        String xml = "<response ip=\"" + queryIp + "\" trans-id=\"" + transactionId + "\" country=\"usa\" city=\"reston\"/>";
        int splitAt = xml.length() / 2;
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0102" + xml.substring(0, splitAt)).getBytes(StandardCharsets.UTF_8),
                ("0202" + xml.substring(splitAt)).getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            XmlResponse response = accessor.queryXml(Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), transactionId);

            assertEquals("usa", response.getResponseMap().get("country"));
            assertEquals("reston", response.getResponseMap().get("city"));
        }
    }

    @Test
    public void query_xmlProtocol_transactionIdMismatch_throwsNetAcuityException() throws Exception {
        String queryIp = "192.0.2.4";
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0101<response ip=\"" + queryIp + "\" trans-id=\"wrong-id\" country=\"usa\"/>")
                        .getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            assertThrows(NetAcuityException.class, () -> accessor.queryXml(
                    Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), "expected-id"));
        }
    }

    @Test
    public void query_xmlProtocol_missingResponseTransactionId_throwsNetAcuityException() throws Exception {
        // The response omits trans-id entirely; a caller can no longer opt out of verification
        // by passing a null/empty transactionId, so this must fail even though the caller-supplied
        // transactionId below is non-empty and would previously have triggered the check anyway.
        String queryIp = "192.0.2.4";
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0101<response ip=\"" + queryIp + "\" country=\"usa\"/>").getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            assertThrows(NetAcuityException.class, () -> accessor.queryXml(
                    Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), "expected-id"));
        }
    }

    @Test
    public void query_xmlProtocol_ipMismatch_throwsNetAcuityException() throws Exception {
        String queryIp = "192.0.2.4";
        String transactionId = "ip-mismatch-1";
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0101<response ip=\"203.0.113.99\" trans-id=\"" + transactionId + "\" country=\"usa\"/>")
                        .getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            assertThrows(NetAcuityException.class, () -> accessor.queryXml(
                    Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), transactionId));
        }
    }

    @Test
    public void query_xmlProtocol_nonLiteralEchoedIp_throwsWithoutResolvingIt() throws Exception {
        String queryIp = "192.0.2.5";
        String transactionId = "ip-not-literal-1";
        // A hostname where the echoed address belongs: rejected outright, so it is never handed to
        // InetAddress.getByName() and never becomes a DNS lookup on the query path.
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0101<response ip=\"attacker.example.com\" trans-id=\"" + transactionId + "\" country=\"usa\"/>")
                        .getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            NetAcuityException e = assertThrows(NetAcuityException.class, () -> accessor.queryXml(
                    Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), transactionId));
            assertEquals("response address is not a valid IP address literal", e.getMessage());
        }
    }

    @Test
    public void query_xmlProtocol_serverReturnsError_throwsNetAcuityException() throws Exception {
        String queryIp = "192.0.2.4";
        String transactionId = "error-1";
        Function<String, byte[][]> responder = request -> new byte[][] {
                ("0101<response ip=\"" + queryIp + "\" trans-id=\"" + transactionId
                        + "\" error=\"database not licensed\"/>").getBytes(StandardCharsets.UTF_8)
        };
        try (FakeNetAcuityServer server = new FakeNetAcuityServer(responder)) {
            DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000, server.getPort());
            NetAcuityException e = assertThrows(NetAcuityException.class, () -> accessor.queryXml(
                    Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                    InetAddress.getByName(queryIp), transactionId));
            assertEquals("database not licensed", e.getMessage());
            assertEquals(true, e.getRawResponse().contains("database not licensed"));
        }
    }

    @Test
    public void query_xmlProtocol_invalidFeatureCode_throwsIllegalArgumentException() throws Exception {
        DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000);
        assertThrows(IllegalArgumentException.class, () -> accessor.queryXml(
                Collections.singletonList(200), InetAddress.getByName("192.0.2.7"), "feature-code-1"));
    }

    @Test
    public void query_xmlProtocol_transactionIdWithQuote_throwsIllegalArgumentException() throws Exception {
        DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000);
        assertThrows(IllegalArgumentException.class, () -> accessor.queryXml(
                Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                InetAddress.getByName("192.0.2.7"), "abc\" ip=\"203.0.113.1"));
    }

    @Test
    public void query_xmlProtocol_transactionIdWithAngleBracket_throwsIllegalArgumentException() throws Exception {
        DbAccessor accessor = DbAccessorFactory.getAccessor(InetAddress.getLoopbackAddress(), 1, 2000);
        assertThrows(IllegalArgumentException.class, () -> accessor.queryXml(
                Collections.singletonList(NetAcuityFeature.GEO.getFeatureCode()),
                InetAddress.getByName("192.0.2.7"), "abc<injected/>"));
    }
}
