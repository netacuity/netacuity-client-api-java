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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * An in-process stand-in for a NetAcuity database server, used to exercise
 * {@code DbAccessorImpl}'s UDP wire protocol handling without a real server.
 * <p>
 * Binds to an OS-assigned ephemeral port on loopback (rather than the fixed NetAcuity
 * protocol port), so tests don't fail if something else already holds that port. Callers
 * must point their {@code DbAccessorImpl}/{@code DbAccessorFactory} instance at {@link #getPort()}.
 * Services exactly one incoming request: the supplied {@code responder} is
 * handed the request payload (decoded as UTF-8) and returns the sequence of
 * response packets to send back to the requester, in order.
 */
final class FakeNetAcuityServer implements AutoCloseable {

    private final DatagramSocket socket;
    private final Thread thread;

    FakeNetAcuityServer(Function<String, byte[][]> responder) throws IOException {
        socket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        thread = new Thread(() -> serve(responder), "fake-netacuity-server");
        thread.setDaemon(true);
        thread.start();
    }

    /** The OS-assigned port this server is listening on. */
    int getPort() {
        return socket.getLocalPort();
    }

    private void serve(Function<String, byte[][]> responder) {
        try {
            byte[] requestBuf = new byte[1500];
            DatagramPacket requestPacket = new DatagramPacket(requestBuf, requestBuf.length);
            socket.receive(requestPacket);
            String request = new String(requestPacket.getData(), requestPacket.getOffset(),
                    requestPacket.getLength(), StandardCharsets.UTF_8);
            for (byte[] responseBytes : responder.apply(request)) {
                DatagramPacket responsePacket =
                        new DatagramPacket(responseBytes, responseBytes.length, requestPacket.getSocketAddress());
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            // expected when close() is called while a receive() is blocked
        }
    }

    @Override
    public void close() throws InterruptedException {
        socket.close();
        thread.join(1000);
    }
}
