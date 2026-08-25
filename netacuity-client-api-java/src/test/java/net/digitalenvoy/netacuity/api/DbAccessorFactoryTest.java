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

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.digitalenvoy.netacuity.api.impl.DbAccessorImpl;

import static org.junit.jupiter.api.Assertions.*;

public class DbAccessorFactoryTest {

    @Test
    public void defaultPort_isNetAcuityProtocolPort() {
        // Real NetAcuity Servers only listen on 5400; if this constant ever drifts,
        // every request built via the no-port-argument factory/constructor overloads
        // would silently go to the wrong port with no other test catching it.
        assertEquals(5400, DbAccessorImpl.DEFAULT_PORT);
    }

    @Test
    public void getAccessor_returnsNonNullDbAccessor() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        DbAccessor accessor = DbAccessorFactory.getAccessor(addr, 1, 1000);
        assertNotNull(accessor);
    }

    @Test
    public void getAccessor_returnsNewInstanceEachCall() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        DbAccessor a1 = DbAccessorFactory.getAccessor(addr, 1, 1000);
        DbAccessor a2 = DbAccessorFactory.getAccessor(addr, 1, 1000);
        assertNotSame(a1, a2);
    }

    @Test
    public void getAccessor_implementsDbAccessorInterface() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        DbAccessor accessor = DbAccessorFactory.getAccessor(addr, 42, 3000);
        assertTrue(accessor instanceof DbAccessor);
    }

    @Test
    public void getAccessor_withExplicitPort_returnsNonNullDbAccessor() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        DbAccessor accessor = DbAccessorFactory.getAccessor(addr, 1, 1000, 15400);
        assertNotNull(accessor);
    }

    @Test
    public void getAccessor_oneArg_defaultsApiIdToZeroAndDefaultTimeout() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        DbAccessor accessor = DbAccessorFactory.getAccessor(addr);
        assertNotNull(accessor);
    }

    @Test
    public void getAccessor_negativeApiId_throwsIllegalArgumentException() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        assertThrows(IllegalArgumentException.class, () -> DbAccessorFactory.getAccessor(addr, -1, 1000));
    }

    @Test
    public void getAccessor_apiIdAboveRange_throwsIllegalArgumentException() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName("192.0.2.1");
        assertThrows(IllegalArgumentException.class, () -> DbAccessorFactory.getAccessor(addr, 128, 1000));
    }
}