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
import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

/**
 * dig NS $delegation
 *
 * @author mieslingert
 */
public class DelegationNSSetLookup implements Runnable {

    private ConcurrentLinkedQueue<Record> queueDelegation;
    private ConcurrentLinkedQueue<Name> queueAAAALookup;
    private ConcurrentLinkedQueue<Name> queueALookup;
    private String resolverToWarm;
    private Cache c;
    private final Logger logger = LoggerFactory.getLogger(DelegationNSSetLookup.class);
    private boolean keepOnRunning = true;

    private DelegationNSSetLookup() {
    }

    public DelegationNSSetLookup(ConcurrentLinkedQueue<Record> queueDelegation,
            ConcurrentLinkedQueue<Name> queueALookup,
            ConcurrentLinkedQueue<Name> queueAAAALookup,
            String resolverToWarm) {
        this.queueDelegation = queueDelegation;
        this.queueALookup = queueALookup;
        this.queueAAAALookup = queueAAAALookup;
        this.resolverToWarm = resolverToWarm;
        this.c = new Cache();
        c.setMaxEntries(0);
    }

    public void run() {
        while (keepOnRunning) {
            try {
                Record delegation = queueDelegation.poll();
                if (delegation != null) {
                    doLookup(delegation);
                } else {
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                logger.warn("Delegation NSSet Lookup Exception: ", e);
            }
        }
    }

    private void doLookup(Record delegation) throws Exception {
        Lookup l = new Lookup(delegation.getName(), Type.NS, DClass.IN);
        l.setResolver(new SimpleResolver(resolverToWarm));
        l.setCache(c);
        l.run();
        logger.debug("querying NS of {}", delegation.getName());
        if (l.getResult() == Lookup.SUCCESSFUL) {
            // dig A and AAAA for every NS record returned
            Record[] answers = l.getAnswers();
            for (int j = 0; j < answers.length; j++) {
                if (answers[j].getType() == Type.NS) {
                    NSRecord ns = (NSRecord) answers[j];
                    queueALookup.add(ns.getTarget());
                    queueAAAALookup.add(ns.getTarget());
                    // TODO: check length and sleep?                 
                }
            }
        } else {
            // Lookup unsuccessful
            logger.warn("query NS for tld delegation {} failed!", delegation);
        }
    }

}
