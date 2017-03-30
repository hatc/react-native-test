package com.basictest;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * implement NotifyListener on NativeModule, so we get could detect then to stop transfer events to RCTDEventEmmiter
 * actually could implement just one @React method in NativeModule - accept ReadableMap and return Promise
 * ReadableMap { method: String, args: ReadableMap } - delegate calls to IObservable
 * i.e. JS function({ method: 'loadPhotos', args: { tag: 'tag0' } })
 * if ReadableMap not contains 'method', reject Promise('"method" required')
 *
 * PromisingMethodDelegate {
 *  boolean canHandle(String method, ReadableMap args, Promise);
 * }
 *
 * Observable {
 *  subscribe(Observer)
 *  unsubscribe(Observer)
 * }
 * ReadableObservable {
 *  toReadable(Object arg)
 * } // ReadableObservableBase implements Observable, ReadableObservable and provide methods to create WriteableMap
 *   // for flat object - i.e. args - Arguments.fromJavaArgs could be used
 * Observer {
 *  notify(Observable, arg)
 * }
 *
 * NativeModule {
 *  subscribeTo(Observable)
 * }
 * */

class Package implements ReactPackage { // MainPackage? )))
    private MainModel model; // DI? Dagger kinda strange...
    Package(MainModel model) {
        if (model == null)
            throw new NullPointerException("model == null");
        this.model = model;
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        PromisingDelegateModule promisingDelegate = new PromisingDelegateModule(reactApplicationContext);
        promisingDelegate.subscribeTo(model);

        return Collections.<NativeModule>singletonList(
                promisingDelegate
        );
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return new ArrayList<>();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }
}
