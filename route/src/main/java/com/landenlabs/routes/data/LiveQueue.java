/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ArrayBlockingQueue;


/**
 * Normal LiveData does not have a queue and only last value is provided.
 * If you post too quickly old values can be over-written.
 * <p>
 * This variant has a thread safe queue of a fixed size.
 * When user gets called into their observer method, they must call next()
 * method to drain queue.
 *
 * <pre><code>
 *  LiveQueue<String> myLiveData = new LiveQueue<>();
 *  myLiveData.observeForever(
 *      state -> { doSomething(state); myLiveData.next(); });  // Forever to get inActive lifeCycle updates
 *  myLiveData.observe(this,
 *      state -> { doSomething(state); myLiveData.next(); });
 * </code></pre>
 */
public class LiveQueue<TT> extends MutableLiveData<TT> {
    public final String name;
    final ArrayBlockingQueue<TT> queue = new ArrayBlockingQueue<>(20);    // Set appropriate size
    volatile boolean postIt = true;
    long postMilli = 0L;

    public LiveQueue() {
        this.name = getClass().getSimpleName();
    }

    public LiveQueue(String name) {
        this.name = name;
    }

    //  Drain queue - if more data will recall observer.
    synchronized
    public void next() {
        if (queue.size() > 0) {
            try {
                super.postValue(queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        postIt = (queue.size() == 0);
    }

    @Override
    public void postValue(@NonNull TT item) {
        try {
            long nowMilli = System.currentTimeMillis();
            if (postIt == false && nowMilli - postMilli > 1000) {
                // Should never happen !!
                // ALog.e.tagMsg(this, name, " Call Next, Post timed out ", String.valueOf(nowMilli - postMilli));
                postIt = true;
            }
            postMilli = nowMilli;

            if (postIt) {
                // Queue is empty - so post it immediately.
                postIt = false;
                super.postValue(item);
            } else {
                // Queue is active - store in queue
                queue.add(item);
            }
        } catch (Exception ignore) {
            // Queue is overflowing, pre-allocate larger queue.
            System.err.println("LiveQueue overflowed");
        }
    }

    synchronized
    public int size() {
        return queue.size();
    }

    synchronized
    public void clear() {
        queue.clear();
    }
}