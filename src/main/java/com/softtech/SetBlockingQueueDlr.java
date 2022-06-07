package com.softtech;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SetBlockingQueueDlr<DLR> extends LinkedBlockingQueue<DLR> {

    private Set<DLR> queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SetBlockingQueueDlr() {
    }

    @Override
    public DLR take() throws InterruptedException {
        DLR t;
        t = super.take();
        queue.remove(t);
        return t;
    }

    @Override
    public synchronized boolean add(DLR dlr) {
        if (queue.contains(dlr)) {
            return false;
        } else {
            queue.add(dlr);
            return super.add(dlr);
        }
    }
}
