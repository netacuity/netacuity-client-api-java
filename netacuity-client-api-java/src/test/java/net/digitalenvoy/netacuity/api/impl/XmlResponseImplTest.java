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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class XmlResponseImplTest {

    @Test
    public void getResponseMap_returnsSuppliedMap() {
        Map<String, String> map = new HashMap<>();
        map.put("country", "usa");
        map.put("city", "atlanta");
        XmlResponseImpl response = new XmlResponseImpl(map, "<response country=\"usa\" city=\"atlanta\"/>");

        assertEquals("usa", response.getResponseMap().get("country"));
        assertEquals("atlanta", response.getResponseMap().get("city"));
    }

    @Test
    public void getRawResponse_returnsSuppliedString() {
        String raw = "<response ip=\"192.0.2.1\" trans-id=\"42\"/>";
        XmlResponseImpl response = new XmlResponseImpl(new HashMap<>(), raw);
        assertEquals(raw, response.getRawResponse());
    }

    @Test
    public void getResponseMap_emptyMap_returnsEmpty() {
        XmlResponseImpl response = new XmlResponseImpl(new HashMap<>(), "");
        assertTrue(response.getResponseMap().isEmpty());
    }

    @Test
    public void getResponseMap_missingKey_returnsNull() {
        XmlResponseImpl response = new XmlResponseImpl(new HashMap<>(), "");
        assertNull(response.getResponseMap().get("nonexistent"));
    }

    @Test
    public void multipleEntries_allAccessible() {
        Map<String, String> map = new HashMap<>();
        map.put("ip", "192.0.2.1");
        map.put("trans-id", "100");
        map.put("country", "usa");
        map.put("city", "testville");
        XmlResponseImpl response = new XmlResponseImpl(map, "raw");

        assertEquals(4, response.getResponseMap().size());
        assertEquals("192.0.2.1", response.getResponseMap().get("ip"));
        assertEquals("100", response.getResponseMap().get("trans-id"));
    }
}