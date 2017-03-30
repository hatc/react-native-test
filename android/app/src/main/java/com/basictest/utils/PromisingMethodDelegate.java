package com.basictest.utils;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

public interface PromisingMethodDelegate {
    boolean canHandle(String method, ReadableMap args, Promise promise);
}
