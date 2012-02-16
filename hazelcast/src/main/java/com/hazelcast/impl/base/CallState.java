/*
 * Copyright (c) 2008-2012, Hazel Bilisim Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.impl.base;

import com.hazelcast.nio.Address;

import java.util.concurrent.locks.ReentrantLock;

public class CallState implements CallStateAware {
    private volatile long callId;
    private final Address caller;
    private final int callerThreadId;
    private final StateQueue<CallStateLog> logQ = new StateQueue<CallStateLog>(100);

    public CallState(long callId, Address caller, int callerThreadId) {
        this.callId = callId;
        this.caller = caller;
        this.callerThreadId = callerThreadId;
    }

    public CallState getCallState() {
        return this;
    }

    public void reset(long callId) {
        this.callId = callId;
        logQ.clear();
    }

    void log(CallStateLog log) {
        logQ.offer(log);
    }

    void logObject(Object obj) {
        log(new CallStateObjectLog(obj));
    }

    public Address getCaller() {
        return caller;
    }

    public int getCallerThreadId() {
        return callerThreadId;
    }

    public long getCallId() {
        return callId;
    }

    public Object[] getLogs() {
        return logQ.copy();
    }

    private static final class StateQueue<E> {
        private final ReentrantLock lock = new ReentrantLock();
        private final int maxSize;
        private final E[] objects;
        int size = 0;

        public StateQueue(int maxSize) {
            this.maxSize = maxSize;
            objects = (E[]) new Object[maxSize];
        }

        public boolean offer(E obj) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if (size >= maxSize) {
                    return false;
                }
                objects[size] = obj;
                size++;
                return true;
            } finally {
                lock.unlock();
            }
        }

        public void clear() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        public Object[] copy() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                Object[] copy = new Object[size];
                System.arraycopy(objects, 0, copy, 0, size);
                return copy;
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CallState [");
        sb.append(callId);
        sb.append("] {");
        sb.append("\ncaller: " + caller);
        sb.append("\nthreadId: " + callerThreadId);
//        for (CallStateLog log : logQ) {
//            sb.append("\n\t" + new Date(log.getDate()) + " : " + log.toString());
//        }
        sb.append("\n}");
        return sb.toString();
    }
}