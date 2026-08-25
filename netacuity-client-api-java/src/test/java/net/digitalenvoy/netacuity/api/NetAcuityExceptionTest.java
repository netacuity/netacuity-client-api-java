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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class NetAcuityExceptionTest {

    @Test
    public void isInstanceOfIOException() {
        assertTrue(new NetAcuityException("test") instanceof IOException);
    }

    @Test
    public void messageConstructor_storesMessage() {
        NetAcuityException ex = new NetAcuityException("bad response");
        assertEquals("bad response", ex.getMessage());
    }

    @Test
    public void causeConstructor_storesMessageAndCause() {
        Throwable cause = new NumberFormatException("not a number");
        NetAcuityException ex = new NetAcuityException("parse failed", cause);
        assertEquals("parse failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void messageConstructor_nullMessage() {
        NetAcuityException ex = new NetAcuityException(null);
        assertNull(ex.getMessage());
    }

    @Test
    public void rawResponse_defaultsToNull() {
        NetAcuityException ex = new NetAcuityException("test");
        assertNull(ex.getRawResponse());
    }

    @Test
    public void rawResponse_roundTrip() {
        NetAcuityException ex = new NetAcuityException("test");
        ex.setRawResponse("5;192.0.2.1;123;;usa");
        assertEquals("5;192.0.2.1;123;;usa", ex.getRawResponse());
    }
}