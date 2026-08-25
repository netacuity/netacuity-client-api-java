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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NetAcuityFeatureTest {

    // ── Feature code correctness ──────────────────────────────────────────────

    @Test
    public void featureCodes_matchExpectedValues() {
        assertEquals(3,  NetAcuityFeature.GEO.getFeatureCode());
        assertEquals(4,  NetAcuityFeature.EDGE.getFeatureCode());
        assertEquals(7,  NetAcuityFeature.ZIP.getFeatureCode());
        assertEquals(8,  NetAcuityFeature.ISP.getFeatureCode());
        assertEquals(9,  NetAcuityFeature.HOME_BUSINESS.getFeatureCode());
        assertEquals(10, NetAcuityFeature.ASN.getFeatureCode());
        assertEquals(11, NetAcuityFeature.LANGUAGE.getFeatureCode());
        assertEquals(12, NetAcuityFeature.PROXY.getFeatureCode());
        assertEquals(14, NetAcuityFeature.IS_AN_ISP.getFeatureCode());
        assertEquals(15, NetAcuityFeature.COMPANY.getFeatureCode());
        assertEquals(16, NetAcuityFeature.DOMAIN_16.getFeatureCode());
        assertEquals(17, NetAcuityFeature.DEMOGRAPHICS.getFeatureCode());
        assertEquals(18, NetAcuityFeature.NAICS.getFeatureCode());
        assertEquals(19, NetAcuityFeature.CBSA.getFeatureCode());
        assertEquals(21, NetAcuityFeature.PULSE_MAX.getFeatureCode());
        assertEquals(24, NetAcuityFeature.MOBILE.getFeatureCode());
        assertEquals(25, NetAcuityFeature.ORGANIZATION.getFeatureCode());
        assertEquals(26, NetAcuityFeature.PULSE.getFeatureCode());
        assertEquals(30, NetAcuityFeature.PULSE_PLUS.getFeatureCode());
        assertEquals(33, NetAcuityFeature.VPN_PROXY.getFeatureCode());
        assertEquals(35, NetAcuityFeature.IPC.getFeatureCode());
        assertEquals(36, NetAcuityFeature.NODIFY.getFeatureCode());
        assertEquals(37, NetAcuityFeature.OBSERVED_COUNTRIES.getFeatureCode());
        assertEquals(40, NetAcuityFeature.AA_SA1.getFeatureCode());
        assertEquals(41, NetAcuityFeature.AAMAX_SA1.getFeatureCode());
        assertEquals(42, NetAcuityFeature.AA_IRIS.getFeatureCode());
        assertEquals(43, NetAcuityFeature.AA_PLZ8.getFeatureCode());
        assertEquals(44, NetAcuityFeature.AA_ZIP4.getFeatureCode());
        assertEquals(45, NetAcuityFeature.AAMAX_ZIP4.getFeatureCode());
        assertEquals(46, NetAcuityFeature.AAMAX_IRIS.getFeatureCode());
        assertEquals(47, NetAcuityFeature.AAMAX_PLZ8.getFeatureCode());
        assertEquals(93, NetAcuityFeature.DECODE.getFeatureCode());
    }

    // ── getFeatureName ────────────────────────────────────────────────────────

    @Test
    public void getFeatureName_returnsEnumName() {
        assertEquals("GEO",    NetAcuityFeature.GEO.getFeatureName());
        assertEquals("PULSE",  NetAcuityFeature.PULSE.getFeatureName());
        assertEquals("DECODE", NetAcuityFeature.DECODE.getFeatureName());
        assertEquals("AA_SA1", NetAcuityFeature.AA_SA1.getFeatureName());
    }

    // ── findForFeatureCode ────────────────────────────────────────────────────

    @Test
    public void findForFeatureCode_knownCodes_returnsCorrectFeature() {
        assertSame(NetAcuityFeature.GEO,    NetAcuityFeature.findForFeatureCode(3));
        assertSame(NetAcuityFeature.EDGE,   NetAcuityFeature.findForFeatureCode(4));
        assertSame(NetAcuityFeature.PULSE,  NetAcuityFeature.findForFeatureCode(26));
        assertSame(NetAcuityFeature.IPC,    NetAcuityFeature.findForFeatureCode(35));
        assertSame(NetAcuityFeature.DECODE, NetAcuityFeature.findForFeatureCode(93));
    }

    @Test
    public void findForFeatureCode_unknownCode_returnsNull() {
        assertNull(NetAcuityFeature.findForFeatureCode(0));
        assertNull(NetAcuityFeature.findForFeatureCode(-1));
        assertNull(NetAcuityFeature.findForFeatureCode(999));
        assertNull(NetAcuityFeature.findForFeatureCode(13)); // gap in feature codes
    }

    // ── Enum completeness ─────────────────────────────────────────────────────

    @Test
    public void enumHas32Values() {
        assertEquals(32, NetAcuityFeature.values().length);
    }

    @Test
    public void allFeatures_havePositiveFeatureCode() {
        for (NetAcuityFeature f : NetAcuityFeature.values()) {
            assertTrue(f.getFeatureCode() > 0, "non-positive featureCode for feature: " + f.name());
        }
    }

    @Test
    public void allFeatureCodes_areUnique() {
        long distinct = java.util.Arrays.stream(NetAcuityFeature.values())
                .mapToInt(NetAcuityFeature::getFeatureCode)
                .distinct()
                .count();
        assertEquals(NetAcuityFeature.values().length, distinct);
    }

    // ── findForFeatureCode is a left inverse of getFeatureCode ───────────────

    @Test
    public void findForFeatureCode_isInverseOfGetFeatureCode() {
        for (NetAcuityFeature f : NetAcuityFeature.values()) {
            assertSame(f, NetAcuityFeature.findForFeatureCode(f.getFeatureCode()));
        }
    }
}