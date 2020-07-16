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

import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

/**
 *
 * @author mieslingert
 */
public class NSAAAALookup implements Runnable {

    private ConcurrentLinkedQueue<Name> queueAAAALookup;
    private String resolverToWarm;
    private boolean keepOnRunning = true;
    private final Logger logger = LoggerFactory.getLogger(NSAAAALookup.class);

    private NSAAAALookup() {

    }

    public NSAAAALookup(ConcurrentLinkedQueue<Name> queueAAAALookup, String resolverToWarm) {
        this.queueAAAALookup = queueAAAALookup;
        this.resolverToWarm = resolverToWarm;
    }

    @Override
    public void run() {
        while (keepOnRunning) {
            try {
                Name n = queueAAAALookup.poll();
                if (n != null) {
                    doLookup(n);
                } else {
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                logger.warn("AAAA Lookup Exception: ", e);
            }
        }

    }

    private void doLookup(Name n) throws Exception {
        logger.debug("Query AAAA for {}", n);
        Lookup la = new Lookup(n, Type.AAAA, DClass.IN);
        la.setResolver(new SimpleResolver(resolverToWarm));
        la.run();

        switch (la.getResult()) {
            case Lookup.SUCCESSFUL:
                logger.debug(la.getAnswers()[0].rdataToString());
                break;
            case Lookup.HOST_NOT_FOUND:
                logger.debug("HOST_NOT_FOUND AAAA record for {}", n);
                break;
            case Lookup.TYPE_NOT_FOUND:
                logger.debug("TYPE_NOT_FOUND AAAA record for {}", n);
                break;
            default:
                logger.warn("query AAAA for NS {} failed!", n);
                break;
        }
    }

}
