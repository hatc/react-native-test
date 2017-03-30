package com.basictest.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BaseObservable {
    protected final Set<Observer> observers = Collections.synchronizedSet(new HashSet<Observer>());

    public void subscribe(Observer observer) { observers.add(observer); }
    public void unsubscribe(Observer observer) { observers.remove(observer); }

    protected void notifyAll(final Event event) {
        synchronized (observers) {
            for (final Observer observer : observers) {
                (new Handler(Looper.getMainLooper())).post(new Runnable() {
                    @Override
                    public void run() {
                        observer.notify(BaseObservable.this, event);
                    }
                });
            }
        }
    }

    public static final class Event {
        private String property, value, oldValue;
        public Event(String property, String value, String oldValue) {
            this.property = property;
            this.value = value;
            this.oldValue = oldValue;
        }

        public String property() { return property; }
        public String value() { return value; }
        public String oldValue() { return oldValue; }
    }
}
