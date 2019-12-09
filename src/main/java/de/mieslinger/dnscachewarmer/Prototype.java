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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.ZoneTransferIn;

/**
 *
 * @author mieslingert
 */
public class Prototype {

    final static Logger logger = LoggerFactory.getLogger(Prototype.class);
    final static String axfrSource = "10.2.215.5";
    final static String resolverToWarm = "10.255.255.3";
    private SimpleResolver res = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // get AXFR of '.'
            ZoneTransferIn xfr = ZoneTransferIn.newAXFR(new Name("."), axfrSource, null);
            List records = xfr.run();
            Name lastSeenName = new Name("abrakadabr.cname");
            // get NS Sets per zone
            for (int i = 0; i < records.size(); i++) {
                Record r = (Record) records.get(i);
                logger.debug("Name: {} type:{}", r.getName(), r.getType());
                if (r.getType() == Type.NS) {
                    if (!lastSeenName.equals(r.getName())) {
                        lastSeenName = r.getName();
                        // dig NS lastSeenName @resolverToWarm
                        Lookup l = new Lookup(lastSeenName, Type.NS, DClass.IN);
                        l.setResolver(new SimpleResolver(resolverToWarm));
                        l.run();
                        logger.info("querying NS of {}", lastSeenName);
                        if (l.getResult() == Lookup.SUCCESSFUL) {
                            // dig A and AAAA for every NS record returned
                            Record[] answers = l.getAnswers();
                            for (int j = 0; j < answers.length; j++) {
                                if (answers[j].getType() == Type.NS) {
                                    NSRecord ns = (NSRecord) answers[j];

                                    logger.info("Query A for {} ({})", ns.getTarget(), lastSeenName);
                                    Lookup la = new Lookup(ns.getTarget(), Type.A, DClass.IN);
                                    la.setResolver(new SimpleResolver(resolverToWarm));
                                    la.run();
                                    if (la.getResult() == Lookup.SUCCESSFUL) {
                                        logger.debug(la.getAnswers()[0].rdataToString());
                                    } else {
                                        logger.warn("query A for tld {} NS {} failed!", lastSeenName, ns.getTarget());
                                    }

                                    logger.info("Query AAAA for {} ({})", ns.getTarget(), lastSeenName);
                                    Lookup laaaa = new Lookup(ns.getTarget(), Type.AAAA, DClass.IN);
                                    laaaa.setResolver(new SimpleResolver(resolverToWarm));
                                    laaaa.run();
                                    if (laaaa.getResult() == Lookup.SUCCESSFUL) {
                                        logger.debug(laaaa.getAnswers()[0].rdataToString());
                                    } else {
                                        logger.warn("query A for tld {} NS {} failed!", lastSeenName, ns.getTarget());
                                    }
                                }
                            }
                        } else {
                            logger.warn("query NS for tld delegation {} failed!", lastSeenName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
