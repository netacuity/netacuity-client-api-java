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

package net.digitalenvoy.netacuityapi.example;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.digitalenvoy.netacuity.api.DbAccessor;
import net.digitalenvoy.netacuity.api.DbAccessorFactory;
import net.digitalenvoy.netacuity.api.NetAcuityFeature;
import net.digitalenvoy.netacuity.api.XmlResponse;
import net.digitalenvoy.netacuity.api.util.IpAddressValidator;

/**
 * Demonstrates a multi-database, single-round-trip query using the XML UDP protocol via
 * {@link DbAccessor#queryXml(List, InetAddress)}.
 */
public class XmlQueryExample {

    // API ID is a customer-assigned integer, range 0-127, default 0; hardcoded here since this
    // is just an example. See DbAccessorFactory.getAccessor(...) below.
    private static final int EXAMPLE_API_ID = 0;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("exactly 3 args are required");
            usage();
            System.exit(1);
        }
        if (!IpAddressValidator.isValid(args[0])) {
            System.out.println("invalid server_ip (must be an IPv4/IPv6 address): " + args[0]);
            usage();
            System.exit(1);
        }
        if (!IpAddressValidator.isValid(args[1])) {
            System.out.println("invalid query_ip (must be an IPv4/IPv6 address): " + args[1]);
            usage();
            System.exit(1);
        }
        InetAddress serverAddr = null;
        InetAddress targetAddress = null;
        List<Integer> featureCodes = new ArrayList<>();
        try {
            serverAddr = InetAddress.getByName(args[0]);
            targetAddress = InetAddress.getByName(args[1]);
            for (String token : args[2].split(",")) {
                int featureCode;
                try {
                    featureCode = Integer.parseInt(token);
                } catch (NumberFormatException nfe) {
                    System.out.println("invalid feature_code (must be an integer): " + token);
                    usage();
                    System.exit(1);
                    return;
                }
                NetAcuityFeature feature = NetAcuityFeature.findForFeatureCode(featureCode);
                if (feature == null) {
                    System.out.println("unrecognized feature code: " + featureCode);
                    usage();
                    System.exit(1);
                }
                featureCodes.add(featureCode);
            }
        } catch (Exception e) {
            System.out.println("invalid argument type: " + e.getMessage());
            usage();
            System.exit(1);
        }
        try {
            int timeoutMillis = 3000;
            DbAccessor accessor = DbAccessorFactory.getAccessor(serverAddr, EXAMPLE_API_ID, timeoutMillis);
            String transactionId = String.valueOf(new SecureRandom().nextInt(1_000_000_000));
            XmlResponse xmlResponse = accessor.queryXml(featureCodes, targetAddress, transactionId);
            Map<String, String> map = xmlResponse.getResponseMap();
            System.out.println("ip = " + map.get("ip"));
            System.out.println("trans-id = " + map.get("trans-id"));
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey().equals("ip") || entry.getKey().equals("trans-id")) {
                    continue;
                }
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            System.out.println("raw-response = " + xmlResponse.getRawResponse());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void usage() {
        System.out.println("usage: <server_ip> <query_ip> <feature_code>[,<feature_code>...]");
        System.out.println("       where feature_code is one of these NetAcuity database feature codes:");
        System.out.println("         feature-code:   3  (geo)");
        System.out.println("         feature-code:   4  (edge)");
        System.out.println("         feature-code:   7  (zip)");
        System.out.println("         feature-code:   8  (isp)");
        System.out.println("         feature-code:   9  (homebiz)");
        System.out.println("         feature-code:  10  (asn)");
        System.out.println("         feature-code:  11  (lang)");
        System.out.println("         feature-code:  12  (proxy)");
        System.out.println("         feature-code:  14  (isisp)");
        System.out.println("         feature-code:  15  (company)");
        System.out.println("         feature-code:  16  (domain16)");
        System.out.println("         feature-code:  17  (demo)");
        System.out.println("         feature-code:  18  (naics)");
        System.out.println("         feature-code:  19  (cbsa)");
        System.out.println("         feature-code:  21  (pulsemax)");
        System.out.println("         feature-code:  24  (mobile)");
        System.out.println("         feature-code:  25  (org)");
        System.out.println("         feature-code:  26  (pulse)");
        System.out.println("         feature-code:  30  (pulseplus)");
        System.out.println("         feature-code:  33  (vpnproxy)");
        System.out.println("         feature-code:  35  (ipc)");
        System.out.println("         feature-code:  36  (nodify)");
        System.out.println("         feature-code:  37  (observedcountries)");
        System.out.println("         feature-code:  40  (aa_sa1)");
        System.out.println("         feature-code:  41  (aamax_sa1)");
        System.out.println("         feature-code:  42  (aa_iris)");
        System.out.println("         feature-code:  43  (aa_plz8)");
        System.out.println("         feature-code:  44  (aa_zip4)");
        System.out.println("         feature-code:  45  (aamax_zip4)");
        System.out.println("         feature-code:  46  (aamax_iris)");
        System.out.println("         feature-code:  47  (aamax_plz8)");
        System.out.println("         feature-code:  93  (decode)");
    }
}
