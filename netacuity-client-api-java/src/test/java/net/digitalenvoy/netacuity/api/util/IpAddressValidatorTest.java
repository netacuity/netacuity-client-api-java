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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IpAddressValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = { "192.0.2.0",
            "192.0.2.4",
            "198.51.100.9",
            "198.51.100.1",
            "198.51.100.11",
            "203.0.113.255",
            "198.51.100.8",
            "203.0.113.0",
            "192.0.2.255" })
    public void acceptsIpv4(final String address) {
        assertTrue(IpAddressValidator.isValid(address));
        assertTrue(IpAddressValidator.isValidIpv4(address));
        assertFalse(IpAddressValidator.isValidIpv6(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "256.0.113.1",          // octet out of range
            "999.0.113.1",
            "1.2.3.256",
            "1.2.3.4.5",            // too many octets
            "1.2.3.",               // empty trailing octet
            ".1.2.3",               // empty leading octet
            "1..2.3",               // empty inner octet
            "1234.1.1.1",           // digit run too long
            "-1.2.3.4",
            "1.2.3.a",
            "1.2.3.4/24",           // CIDR suffix
            " 1.2.3.4",             // surrounding whitespace
            "1.2.3.4 ",
            "example.com",
            "" })
    public void rejectsMalformedIpv4(final String address) {
        assertFalse(IpAddressValidator.isValid(address));
        assertFalse(IpAddressValidator.isValidIpv4(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "::",
            "::1",
            "1::",
            "2001:db8::9",
            "0:0:0:0:0:0:0:0",
            "1:2:3:4:5:6:7:8",
            "2001:db8:3:4:5:6:7::",                     // "::" standing in for the eighth group
            "2001:db8:85a3:0:0:8a2e:370:7334",
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "2001:db8::8a2e:370:7334",
            "2001:DB8::abcd",                           // mixed-case hex
            "2001:db8:0:0:200:f8ff:fe21:67cf" })
    public void acceptsIpv6(final String address) {
        assertTrue(IpAddressValidator.isValid(address));
        assertTrue(IpAddressValidator.isValidIpv6(address));
        assertFalse(IpAddressValidator.isValidIpv4(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "::ffff:198.51.100.1",                      // IPv4-mapped
            "::ffff:192.0.2.0",
            "::198.51.100.1",                           // IPv4-compatible
            "64:ff9b::198.51.100.4",                    // NAT64 prefix
            "0:0:0:0:0:ffff:1.2.3.4",                   // uncompressed, quad fills the low 32 bits
            "1:2:3:4:5:6:1.2.3.4" })
    public void acceptsIpv6WithIpv4Tail(final String address) {
        assertTrue(IpAddressValidator.isValid(address));
        assertTrue(IpAddressValidator.isValidIpv6(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1:2:3:4:5:6:7:8:9",        // too many groups
            "1:2:3:4:5:6:7",            // too few groups without "::"
            "1:2:3:4:5:6:7:8::",        // "::" with nothing left to stand in for
            "::2001:db8:1:2:3:4:5:6",
            "2001:db8::2::3",           // more than one "::"
            "::2001:db8::2",
            ":::",
            "1:::2",
            ":",
            ":1:2:3:4:5:6:7:8",         // lone leading colon
            "1:2:3:4:5:6:7:8:",         // lone trailing colon
            "12345::",                  // hex run too long
            "gggg::1",                  // not hex
            "٣::1",                     // non-ASCII digit
            "1:2:3:4:5:6:7:1.2.3.4",    // IPv4 tail pushes past eight groups
            "1.2.3.4::",                // IPv4 not in the tail position
            "::198.51.100.4:5",
            "::198.51.100",             // truncated quad
            "1::2:3.4.5.6.7",
            "1:.2::",
            "::.",
            "2001:db8::/32",            // CIDR suffix
            "1:2:3:4:5:6:7:8 ",         // surrounding whitespace
            " ::1",
            "" })
    public void rejectsMalformedIpv6(final String address) {
        assertFalse(IpAddressValidator.isValid(address));
        assertFalse(IpAddressValidator.isValidIpv6(address));
    }

    /**
     * These are the forms where this validator deliberately disagrees with {@code InetAddress.getByName(String)}, which
     * accepts every one of them as a literal. The strictness is the point of the class, so each case is pinned here
     * rather than left to the generic malformed-input tests: a future change that "fixes" the validator to match
     * {@code InetAddress} should fail loudly.
     * <p>
     * The reason for rejecting them is that each has more than one reading, and which reading applies depends on who
     * does the parsing. {@code 0198.51.100.5} is 198.51.100.5 to Java, which reads the leading zero as decimal, and a
     * different host to {@code inet_aton}, which reads it as octal; {@code 0198.51.100.11} is similarly ambiguous. The
     * shorthand forms are worse: {@code 192.513} and {@code 3221225985} both mean 192.0.2.1, so a value that does not
     * look like an address at all still reaches a host. An address literal that is validated here and resolved
     * somewhere else must mean exactly one host in both places, and these do not.
     * <p>
     * Zone ids and brackets are rejected for a different reason: they are address syntax that carries something other
     * than the address ({@code %1} an interface, {@code [...]} a URL authority), which the caller should strip before
     * validating rather than have this class silently ignore.
     * <p>
     * This test deliberately does not call {@code InetAddress} to prove the disagreement. What that method accepts
     * varies by JDK, and the zone-id cases additionally depend on the interfaces of the host running the build, which
     * would make the comparison flaky. The verified behavior is recorded above instead.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "0203.0.113.44",            // InetAddress: 203.0.113.44   inet_aton: differs
            "0198.51.100.11",           // InetAddress: 198.51.100.11  inet_aton: differs
            "198.051.100.4",            // InetAddress: 198.51.100.4   inet_aton: 198.51.100.4
            "198.51.100.04",            // InetAddress: 198.51.100.4   inet_aton: 198.51.100.4
            "0198.51.100.5",            // InetAddress: 198.51.100.5   inet_aton: differs
            "198.51.100.4.",            // InetAddress: 198.51.100.4, trailing dot ignored
            "198.51.25607",             // InetAddress: 198.51.100.7, three-part shorthand
            "198.3367945",              // InetAddress: 198.51.100.9, two-part shorthand
            "192.513",                  // InetAddress: 192.0.2.1
            "3221225985",               // InetAddress: 192.0.2.1, bare 32-bit integer
            "0",                        // InetAddress: 0.0.0.0
            "::ffff:0198.51.100.4",     // InetAddress: 198.51.100.4, leading zero inside the IPv4 tail
            "::0198.51.100.4",          // InetAddress: 0:0:0:0:0:0:c633:6404
            "64:ff9b::198.51.100.04",   // InetAddress: 64:ff9b:0:0:0:0:c633:6404
            "[::1]",                    // InetAddress: 0:0:0:0:0:0:0:1, bracketed URL-authority form
            "[2001:db8::9]",            // InetAddress: 2001:db8:0:0:0:0:0:9
            "2001:db8::1%1",            // InetAddress: 2001:db8:0:0:0:0:0:1%1, numeric zone id
            "2001:db8::1%eth0" })       // InetAddress: accepted when the host has an eth0 interface
    public void rejectsAmbiguousFormsInetAddressAccepts(final String address) {
        assertFalse(IpAddressValidator.isValid(address));
        assertFalse(IpAddressValidator.isValidIpv4(address));
        assertFalse(IpAddressValidator.isValidIpv6(address));
    }

    @Test
    public void rejectsNull() {
        assertFalse(IpAddressValidator.isValid(null));
        assertFalse(IpAddressValidator.isValidIpv4(null));
        assertFalse(IpAddressValidator.isValidIpv6(null));
    }
}
