package com.dotmarketing.portlets.contentlet.business;

interface Flushable<T> {
    void flushAll();
    void flush(T t);
}
