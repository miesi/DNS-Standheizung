/*
 * The MIT License
 *
 * Copyright 2019 mieslingert.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.mieslinger.dnscachewarmer;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.ZoneTransferIn;

/**
 *
 * @author mieslingert
 */
public class Main {

    @Argument(alias = "s", description = "AXFR source for '.' Zone")
    private static String axfrSource = "iad.xfr.dns.icann.org";

    @Argument(alias = "r", description = "Resolver to query")
    private static String resolverToWarm = "172.19.254.4";

    @Argument(alias = "nt", description = "Number of Threads for NS lookups")
    private static int numThreadsNSLookup = 5;

    @Argument(alias = "at", description = "Number of Threads for AAAA lookups")
    private static int numThreadsALookup = 25;

    @Argument(alias = "aaaat", description = "Number of Threads for AAAA lookups")
    private static int numThreadsAAAALookup = 25;
    
    //@Argument(alias = "t", description = "resolver timeout (seconds)")
    //private static int timeout = 2;
    

    @Argument(alias = "d", description = "enable debug")
    private static boolean debug = false;

    private static final ConcurrentLinkedQueue<Record> queueDelegation = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Name> queueALookup = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Name> queueAAAALookup = new ConcurrentLinkedQueue<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);

        List<String> unparsed = Args.parseOrExit(Main.class, args);

        Name lastSeenName = null;
        List records = null;

        setupWorkerThreads();

        try {
            ZoneTransferIn xfr = ZoneTransferIn.newAXFR(new Name("."), axfrSource, null);
            records = xfr.run();
            lastSeenName = new Name("abrakadabr.cname");
        } catch (Exception e) {
            logger.error("AXFR failed: {}, exiting", e.getMessage());
            System.exit(1);
        }

        // get Delegatios from root zone
        // One Shot implementation
        for (int i = 0; i < records.size(); i++) {
            Record r = (Record) records.get(i);
            logger.debug("Delegation: {}", r.getName());
            if (r.getType() == Type.NS) {
                if (!lastSeenName.equals(r.getName())) {
                    lastSeenName = r.getName();
                    queueDelegation.add(r);
                    logger.info("delegation {} queued", r.getName());
                }
            }
        }
        while (true) {
            try {
                Thread.sleep(5000);
                logger.info("delegation queue {}, A queue {}, AAAA queue {}", queueDelegation.size(), queueALookup.size(), queueAAAALookup.size());
            } catch (Exception e) {
                logger.warn("sleep interrupted: {}", e.getMessage());
            }
        }
    }

    private static void setupWorkerThreads() {
        for (int i = 0; i < numThreadsNSLookup; i++) {
            Thread tNSLookup = new Thread(new DelegationNSSetLookup(queueDelegation, queueALookup, queueAAAALookup, resolverToWarm));
            tNSLookup.setDaemon(true);
            tNSLookup.setName("DelegationNSSetLookup-" + i);
            tNSLookup.start();
        }
        for (int i = 0; i < numThreadsALookup; i++) {
            Thread tNSLookup = new Thread(new NSALookup(queueALookup, resolverToWarm));
            tNSLookup.setDaemon(true);
            tNSLookup.setName("NSALookup-" + i);
            tNSLookup.start();
        }
        for (int i = 0; i < numThreadsAAAALookup; i++) {
            Thread tNSLookup = new Thread(new NSAAAALookup(queueAAAALookup, resolverToWarm));
            tNSLookup.setDaemon(true);
            tNSLookup.setName("NSAAAALookup-" + i);
            tNSLookup.start();
        }
    }
}
