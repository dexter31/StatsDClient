java-statsd-client
==================

A statsd java client extended from tim groups implementation using vertx i/o.
```

Usage
-----
```java

import com.hp.wfm.statsd.NIOStatsDClient;

public class Test {

    private static final int STATSD_SERVER_PORT = 8125;         //needs to be a config
    private static String environment = "dev";
    private static String asset = "wfm";
    private static final String STATSD_SERVER_HOST = "c0039559.itcs.hp.com";

    private static final NIOStatsDClient client = new NIOStatsDClient(environment+"."+asset, STATSD_SERVER_HOST, STATSD_SERVER_PORT);

    public static void main(String args[]) {

        client.recordExecutionTime("ActionProcessor.Response", new Long("53421"));
        client.increment("ActionProcessor.200");
        client.gauge("ActionProcessor.Worklist.Response.size" , new Long("1233"));
        //client.recordSetEvent("ActionProcessor.Worklist.Response.size" , new Long("1233"));

        try {
            Thread.sleep(new Long(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


