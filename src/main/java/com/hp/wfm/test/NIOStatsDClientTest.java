package com.hp.wfm.test;

import com.hp.wfm.statsd.NIOStatsDClient;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Long.valueOf;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;

public final class NIOStatsDClientTest {

    private static final int STATSD_SERVER_PORT = 8125;

    private final NIOStatsDClient client = new NIOStatsDClient("localhost.test.wfm", "c0039559.itcs.hp.com", STATSD_SERVER_PORT);
    private final DummyStatsDServer server = new DummyStatsDServer(STATSD_SERVER_PORT);

    @After
    public void stop() throws Exception {
        client.stop();
        server.stop();
    }

    @Test(timeout=5000L) public void
    sends_counter_value_to_statsd() throws Exception {
        client.count("mycount", Long.MAX_VALUE);
        server.waitForMessage();
        
    }

    @Test(timeout=5000L) public void
    sends_counter_value_with_rate_to_statsd() throws Exception {
        client.count("mycount", Long.MAX_VALUE, 0.00024);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_counter_increment_to_statsd() throws Exception {
        client.incrementCounter("myinc");
        server.waitForMessage();
        
    }

    @Test(timeout=5000L) public void
    sends_counter_decrement_to_statsd() throws Exception {
        client.decrementCounter("mydec");
        server.waitForMessage();
        
    }

    @Test(timeout=5000L) public void
    sends_gauge_to_statsd() throws Exception {
        client.recordGaugeValue("mygauge", Long.MAX_VALUE);
        server.waitForMessage();
        
    }

    @Test(timeout=5000L) public void
    sends_fractional_gauge_to_statsd() throws Exception {
        client.recordGaugeValue("mygauge", 423.123456789d);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_large_fractional_gauge_to_statsd() throws Exception {
        client.recordGaugeValue("mygauge", 423423423.9d);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_zero_gauge_to_statsd() throws Exception {
        client.recordGaugeValue("mygauge", 0L);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_negagive_gauge_to_statsd_by_resetting_to_zero_first() throws Exception {
        client.recordGaugeValue("mygauge", -423L);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_gauge_positive_delta_to_statsd() throws Exception {
        client.recordGaugeDelta("mygauge", 423L);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_gauge_negative_delta_to_statsd() throws Exception {
        client.recordGaugeDelta("mygauge", -423L);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_gauge_zero_delta_to_statsd() throws Exception {
        client.recordGaugeDelta("mygauge", 0L);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    sends_set_to_statsd() throws Exception {
        client.recordSetEvent("myset", "test");
        server.waitForMessage();
        
    }

    @Test(timeout=5000L) public void
    sends_timer_to_statsd() throws Exception {
        client.recordExecutionTime("mytime", 123L);
        server.waitForMessage();
        
    }

    @Test(timeout=5000L) public void
    sends_timer_with_rate_to_statsd() throws Exception {
        client.recordExecutionTime("mytime", 123L, 0.000123);
        server.waitForMessage();

    }

    /*@Test(timeout=5000L) public void
    sends_timer_to_statsd_based_on_specified_start_time_to_now() throws Exception {
        final long startTime = System.currentTimeMillis() - 1000L;

        final long beforeCompletionTime = System.currentTimeMillis();
        client.recordExecutionTimeToNow("mytime", startTime);
        final long afterCompletionTime = System.currentTimeMillis();

        server.waitForMessage();

        long maxExpectedValue = afterCompletionTime - startTime;
        long minExpectedValue = beforeCompletionTime - startTime;
        final String messageReceived = server.messagesReceived().get(0);
        final Matcher resultMatcher = Pattern.compile(".*:(\\d+)\\|ms").matcher(messageReceived);
        assertTrue(messageReceived, resultMatcher.matches());
        assertThat(valueOf(resultMatcher.group(1)), Matchers.greaterThanOrEqualTo(minExpectedValue));
        assertThat(valueOf(resultMatcher.group(1)), Matchers.lessThanOrEqualTo(maxExpectedValue));
    }*/

    @Test(timeout=5000L) public void
    sends_timer_of_zero_to_statsd_based_on_specified_start_time_in_the_future() throws Exception {
        client.recordExecutionTimeToNow("mytime", System.currentTimeMillis() + 100000L);
        server.waitForMessage();

    }

    @Test(timeout=5000L) public void
    allows_empty_prefix() {
        final NIOStatsDClient emptyPrefixClient = new NIOStatsDClient(" ", "localhost", STATSD_SERVER_PORT);
        try {
            emptyPrefixClient.count("mycount", 24L);
            server.waitForMessage();
        } finally {
            emptyPrefixClient.stop();
        }
        assertThat(server.messagesReceived(), contains(startsWith("mycount:")));
    }

    @Test(timeout=5000L) public void
    allows_null_prefix() {
        final NIOStatsDClient nullPrefixClient = new NIOStatsDClient(null, "localhost", STATSD_SERVER_PORT);
        try {
            nullPrefixClient.count("mycount", 24L);
            server.waitForMessage();
        } finally {
            nullPrefixClient.stop();
        }
    }

    private static final class DummyStatsDServer {
        private final List<String> messagesReceived = new ArrayList<String>();
        private final DatagramSocket server;

        public DummyStatsDServer(int port) {
            try {
                server = new DatagramSocket(port);
            } catch (SocketException e) {
                throw new IllegalStateException(e);
            }
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        final DatagramPacket packet = new DatagramPacket(new byte[256], 256);
                        server.receive(packet);
                        messagesReceived.add(new String(packet.getData(), Charset.forName("UTF-8")).trim());
                    } catch (Exception e) { }
                }
            }).start();
        }

        public void stop() {
            server.close();
        }

        public void waitForMessage() {
                    }

        public List<String> messagesReceived() {
            return new ArrayList<String>(messagesReceived);
        }
    }
}