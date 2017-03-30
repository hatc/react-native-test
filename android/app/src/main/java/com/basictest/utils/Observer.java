package com.basictest.utils;

public interface Observer {
    void notify(BaseObservable observable, BaseObservable.Event event);
}
