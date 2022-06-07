package com.softtech;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class SetBlockingQueue<T> extends PriorityBlockingQueue<T> {

    public SetBlockingQueue() {
        super();
    }

    public SetBlockingQueue(int initialCapacity) {
        super(initialCapacity);
    }

    public SetBlockingQueue(Collection<? extends T> c) {
        super(c);
    }

    public SetBlockingQueue(int initialCapacity, Comparator<? super T> comparator) {
        super(initialCapacity, comparator);
    }

    @Override
    public T take() throws InterruptedException {
        return super.take();
    }

    @Override
    public synchronized boolean add(T sms) {
        return super.add(sms);
    }
}
