package org.opengauss.portalcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Thread get order.
 */
public class ThreadGetOrder extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadCheckProcess.class);
    /**
     * The Exit.
     */
    public boolean exit = false;

    @Override
    public void run() {
        while (!exit && !Plan.stopPlan) {
            Tools.readInputOrder();
            Tools.sleepThread(1000, "getting order");
        }
    }
}
