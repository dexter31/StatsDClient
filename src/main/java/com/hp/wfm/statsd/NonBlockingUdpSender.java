package com.hp.wfm.statsd;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.platform.Verticle;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Naraen
 * @version 1.0
 * @since 22 Aug 2014
 *
 * A simple UDP sender that uses Vertx datagram to send data to statsd server
 * using NIO.
 */
public final class NonBlockingUdpSender extends Verticle {
    private static DatagramSocket socket;
    private static String host;
    private static int port;
    private StatsDClientErrorHandler handler;

    public NonBlockingUdpSender(String hostname, int port, Charset encoding, StatsDClientErrorHandler handler) throws IOException {
        vertx = VertxFactory.newVertx();
        this.handler = handler;
        this.host = hostname;
        this.port = port;
        this.socket = vertx.createDatagramSocket(InternetProtocolFamily.IPv4);
    }

    public void stop() {
        try {
            socket.close();
        } catch (Exception e) {
            handler.handle(e);
        }
    }

    public void send(final String message) {
        socket.send(message, host, port, new AsyncResultHandler<DatagramSocket>() {
            public void handle(AsyncResult<DatagramSocket> asyncResult) {
                //Do nothing. Dont care about the result.
            }
        });
    }
}