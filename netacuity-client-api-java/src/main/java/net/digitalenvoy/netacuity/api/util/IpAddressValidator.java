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
package net.digitalenvoy.netacuity.api.util;

/**
 * Strict IPv4/IPv6 address literal check with no hostname or DNS fallback. Unlike
 * {@code InetAddress.getByName(String)}, which resolves anything that is not a recognized literal as a hostname over
 * the network, this rejects non-literal input outright, so a malformed address never triggers a DNS lookup.
 */
public final class IpAddressValidator {
    private IpAddressValidator() {
    }

    /**
     * Returns true if the given string is a valid IPv4 or IPv6 address literal. A colon anywhere in the string selects
     * the IPv6 form, since no valid IPv4 literal contains one.
     */
    public static boolean isValid(final String s) {
        return s != null && (s.indexOf(':') < 0 ? ipv4(s, 0) : ipv6(s));
    }

    /** Returns true if the given string is a valid IPv4 address literal. */
    public static boolean isValidIpv4(final String s) {
        return s != null && ipv4(s, 0);
    }

    /** Returns true if the given string is a valid IPv6 address literal. */
    public static boolean isValidIpv6(final String s) {
        return s != null && ipv6(s);
    }

    /** Steps {@code s[from..]}: four runs of 1-3 digits, each 0-255 and free of leading zeros, separated by dots. */
    private static boolean ipv4(final String s, final int from) {
        int dots = 0, run = 0, val = 0;
        boolean zeroLead = false;
        for (int i = from; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                if (++run > 3 || (run == 2 && zeroLead)) return false;
                if (run == 1) zeroLead = c == '0';
                if ((val = val * 10 + c - '0') > 255) return false;
            } else if (c == '.') {
                if (run == 0 || ++dots > 3) return false;
                run = val = 0;
            } else {
                return false;
            }
        }
        return dots == 3 && run > 0;
    }

    /** Steps {@code s}: runs of 1-4 hex digits separated by ':', at most one "::", with an optional IPv4 tail. */
    private static boolean ipv6(final String s) {
        if (s.startsWith(":") && !s.startsWith("::")) return false;     // lone leading ':'
        if (s.endsWith(":") && !s.endsWith("::")) return false;         // lone trailing ':'
        int groups = 0, run = 0, colons = 0;
        boolean compressed = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == ':') {
                if (run > 0) {
                    groups++;
                    run = 0;
                }
                if (++colons > 2 || (colons == 2 && compressed)) return false;
                compressed |= colons == 2;
            } else if (c == '.') {
                return ipv4(s, i - run) && fits(groups + 2, compressed);    // dotted quad = low 32 bits
            } else if ((c >= '0' && c <= '9') || ((c | 32) >= 'a' && (c | 32) <= 'f')) {
                if (++run > 4) return false;
                colons = 0;
            } else {
                return false;
            }
        }
        if (run > 0) groups++;
        return fits(groups, compressed);
    }

    /** "::" must stand in for at least one group; without it, all eight must be written out. */
    private static boolean fits(final int groups, final boolean compressed) {
        return compressed ? groups < 8 : groups == 8;
    }
}
