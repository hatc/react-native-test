package com.basictest;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.basictest.utils.BaseObservable;
import com.basictest.utils.Observer;
import com.basictest.utils.PromisingMethodDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ReactModule(name = "PromisingDelegate")
public class PromisingDelegateModule
        extends ReactContextBaseJavaModule
        implements LifecycleEventListener, Observer {
    private static final Logger logger = LoggerFactory.getLogger(PromisingDelegateModule.class);

    private static final String ERROR_UNKNOWN_METHOD = "E_UNKNOWN_METHOD";

    public PromisingDelegateModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "PromisingDelegate";
    }

    @Override
    public void initialize() {
        getReactApplicationContext().addLifecycleEventListener(this);
    }

    private final List<WeakReference<PromisingMethodDelegate>> delegates =
            Collections.synchronizedList(new ArrayList<WeakReference<PromisingMethodDelegate>>());
    public void subscribeTo(BaseObservable observable) {
        observable.subscribe(this);

        if (observable instanceof PromisingMethodDelegate) {
            delegates.add(new WeakReference<>((PromisingMethodDelegate)observable));
        }
    }

    @ReactMethod
    public void invokeDelegate(String method, ReadableMap args, Promise promise) {
        synchronized (delegates) {
            for (WeakReference<PromisingMethodDelegate> delegateRef : delegates) {
                PromisingMethodDelegate delegate = delegateRef.get();
                if (delegate != null && delegate.canHandle(method, args, promise)) {
                    return;
                }
            }
        }
        logger.warn("invokeDelegate(): no delegate registered for method \"{}\"", method);
        promise.reject(ERROR_UNKNOWN_METHOD, "No delegate registered for such method");
    }

    private boolean isHostResumed;
    @Override
    public void notify(BaseObservable observable, BaseObservable.Event event) {
        if (!(getReactApplicationContext().hasActiveCatalystInstance() && isHostResumed)) {
            logger.warn("dropping event, ReactApplicationContext unavailable");
            return;
        }

        logger.info("event.property(\"{}\")", event.property());

        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(observable.getClass().getName(), event2Map(event));
    }

    @Override
    public void onHostResume() {
        isHostResumed = true;
    }

    @Override
    public void onHostPause() {
        isHostResumed = false;
    }

    @Override
    public void onHostDestroy() {
        isHostResumed = false;

        synchronized (delegates) {
            for (WeakReference<PromisingMethodDelegate> delegateRef : delegates) {
                PromisingMethodDelegate delegate = delegateRef.get();
                if (delegate != null)
                    ((BaseObservable)delegate).unsubscribe(this);
            }
            delegates.clear();
        }
    }

    static ReadableMap event2Map(BaseObservable.Event event) {
        WritableMap map = Arguments.createMap();
        map.putString("property", event.property());
        map.putString("value", event.value());
        map.putString("oldValue", event.oldValue());
        return map;
    }
    static BaseObservable.Event map2event(ReadableMap map) {
        String property, value, oldValue;
        try {
            property = map.getString("property");
            value = map.getString("value");
            oldValue = map.getString("oldValue");
        } catch (NoSuchKeyException e) {
            logger.error("map2event(): required property is missed in provided ReadableMap", e);
            throw new IllegalArgumentException("required property is missed in provided ReadableMap", e);
        }
        return new BaseObservable.Event(property, value, oldValue);
    }
}
