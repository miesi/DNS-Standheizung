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

import static de.mieslinger.dnscachewarmer.Prototype.logger;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

/**
 *
 * @author mieslingert
 */
public class NSALookup implements Runnable {

    private ConcurrentLinkedQueue<Name> queueALookup;
    private String resolverToWarm;
    private boolean keepOnRunning = true;
    private final Logger logger = LoggerFactory.getLogger(NSALookup.class);

    private NSALookup() {

    }

    public NSALookup(ConcurrentLinkedQueue<Name> queueALookup, String resolverToWarm) {
        this.queueALookup = queueALookup;
        this.resolverToWarm = resolverToWarm;
    }

    @Override
    public void run() {
        while (keepOnRunning) {
            try {
                Name n = queueALookup.poll();
                if (n != null) {
                    doLookup(n);
                } else {
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                logger.warn("A Lookup Exception: ", e);
            }
        }

    }

    private void doLookup(Name n) throws Exception {
        logger.info("Query A for {}", n);
        Lookup la = new Lookup(n, Type.A, DClass.IN);
        la.setResolver(new SimpleResolver(resolverToWarm));
        la.run();
        if (la.getResult() == Lookup.SUCCESSFUL) {
            logger.debug(la.getAnswers()[0].rdataToString());
        } else {
            logger.warn("query A for NS {} failed!", n);
        }
    }

}
