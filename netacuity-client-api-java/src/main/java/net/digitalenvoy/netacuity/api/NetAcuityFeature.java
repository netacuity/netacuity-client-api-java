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

/**
 * Enumerates the NetAcuity features, feature codes, and query types.
 */
public enum NetAcuityFeature implements FeatureInfo {
    GEO(3),
    EDGE(4),
    ZIP(7),
    ISP(8),
    HOME_BUSINESS(9),
    ASN(10),
    LANGUAGE(11),
    PROXY(12),
    IS_AN_ISP(14),
    COMPANY(15),
    DOMAIN_16(16),
    DEMOGRAPHICS(17),
    NAICS(18),
    CBSA(19),
    PULSE_MAX(21),
    MOBILE(24),
    ORGANIZATION(25),
    PULSE(26),
    PULSE_PLUS(30),
    VPN_PROXY(33),
    IPC(35),
    NODIFY(36),
    OBSERVED_COUNTRIES(37),
    AA_SA1(40),
    AAMAX_SA1(41),
    AA_IRIS(42),
    AA_PLZ8(43),
    AA_ZIP4(44),
    AAMAX_ZIP4(45),
    AAMAX_IRIS(46),
    AAMAX_PLZ8(47),
    DECODE(93);

    private final int featureCode;

    private NetAcuityFeature(int featureCode) {
        this.featureCode = featureCode;
    }

    @Override
    /** Get the unique name of the feature. */
    public String getFeatureName() {
        return name();
    }

    /** Get the feature code. */
    @Override
    public int getFeatureCode() {
        return featureCode;
    }

    /**
     * Get the {@link NetAcuityFeature} that corresponds to the given feature code.
     *
     * @param featureCode The feature code.
     * @return The {@link NetAcuityFeature} or null if there is no match for the feature code.
     */
    public static NetAcuityFeature findForFeatureCode(int featureCode) {
        for (NetAcuityFeature feature : values()) {
            if (featureCode == feature.featureCode) {
                return feature;
            }
        }
        return null;
    }

}
