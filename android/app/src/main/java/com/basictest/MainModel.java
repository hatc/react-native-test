package com.basictest;

import android.os.AsyncTask;
import android.os.Looper;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.basictest.utils.BaseObservable;
import com.basictest.utils.PromisingMethodDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

class MainModel // such a profound class name...
        extends BaseObservable
        implements PromisingMethodDelegate {
    private static final Logger logger = LoggerFactory.getLogger(MainModel.class);

    private static final String ERROR_ILLEGAL_ARGUMENT = "E_ILLEGAL_ARGUMENT";

    // event args: { property:String, value:String, oldValue:String }
    // events: # also are properties
    //  busy: boolean
    //  tag:  string # https://developer.android.com/training/keyboard-input/style.html - editText.setOnEditorActionListener
    //  view: string # main - start screen, photos - ReactRootView in full screen

    private boolean busy;
    public boolean busy() { return busy; }
    private void setBusy(boolean v) {
        if (busy != v) {
            busy = v;
            notifyAll(new Event("busy", Boolean.toString(busy), Boolean.toString(!busy)));
        }
    }

    private String tag = "";
    public String tag() { return tag; }
    public void setTag(String v) {
        if (v == null)
            v = "";
        // if (!(v == null ? tag == null : v.equalsIgnoreCase(tag))) {
        if (!v.equalsIgnoreCase(tag)) {
            final Event event = new Event("tag", v, tag);
            tag = v;
            notifyAll(event);
        }
    }

    private void getHotImpl() {
        setBusy(true);
        Random rnd = new Random();
        try {
            Thread.sleep(0x1000 + rnd.nextInt(0x100));
        } catch (InterruptedException ignored) {}
        StringBuilder s = new StringBuilder();
        for (int i = 0, n = 3 + rnd.nextInt(20); i < n; ++i) {
            s.append((char)((int)'a' + rnd.nextInt((int)'z' - (int)'a' + 1)));
        }
        setTag(s.toString()); // setTag("hot");
        setBusy(false);
    }
    public void getHot() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    MainModel.this.getHotImpl();
                    return null;
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            getHotImpl();
        }
    }

    // searchImpl
    // search

    private static final String[] sImages = {
            "https://lh6.googleusercontent.com/-55osAWw3x0Q/URquUtcFr5I/AAAAAAAAAbs/rWlj1RUKrYI/s1024/A%252520Photographer.jpg",
            "https://lh4.googleusercontent.com/--dq8niRp7W4/URquVgmXvgI/AAAAAAAAAbs/-gnuLQfNnBA/s1024/A%252520Song%252520of%252520Ice%252520and%252520Fire.jpg",
            "https://lh5.googleusercontent.com/-7qZeDtRKFKc/URquWZT1gOI/AAAAAAAAAbs/hqWgteyNXsg/s1024/Another%252520Rockaway%252520Sunset.jpg",
            "https://lh3.googleusercontent.com/--L0Km39l5J8/URquXHGcdNI/AAAAAAAAAbs/3ZrSJNrSomQ/s1024/Antelope%252520Butte.jpg",
            "https://lh6.googleusercontent.com/-8HO-4vIFnlw/URquZnsFgtI/AAAAAAAAAbs/WT8jViTF7vw/s1024/Antelope%252520Hallway.jpg",
            "https://lh4.googleusercontent.com/-WIuWgVcU3Qw/URqubRVcj4I/AAAAAAAAAbs/YvbwgGjwdIQ/s1024/Antelope%252520Walls.jpg",
            "https://lh6.googleusercontent.com/-UBmLbPELvoQ/URqucCdv0kI/AAAAAAAAAbs/IdNhr2VQoQs/s1024/Apre%2525CC%252580s%252520la%252520Pluie.jpg",
            "https://lh3.googleusercontent.com/-s-AFpvgSeew/URquc6dF-JI/AAAAAAAAAbs/Mt3xNGRUd68/s1024/Backlit%252520Cloud.jpg",
            "https://lh5.googleusercontent.com/-bvmif9a9YOQ/URquea3heHI/AAAAAAAAAbs/rcr6wyeQtAo/s1024/Bee%252520and%252520Flower.jpg",
            "https://lh5.googleusercontent.com/-n7mdm7I7FGs/URqueT_BT-I/AAAAAAAAAbs/9MYmXlmpSAo/s1024/Bonzai%252520Rock%252520Sunset.jpg"
    };
    private static final String[] sNames = {
            "photographer",
            "songoficeandfire",
            "rockawaysunset",
            "antelopebutte",
            "antelopehallway",
            "antelopewalls",
            "aprelapluie",
            "backlitcloud",
            "beeandflower",
            "bonzairocksunset"
    };
    private void getPhotosImpl(String query, Promise promise) {
        setBusy(true);
        try {
            Thread.sleep(0x1000 + (new Random()).nextInt(0x100));
        } catch (InterruptedException ignored) {}

        final WritableArray photos = Arguments.createArray();
        for (int i = 0; i < sImages.length; ++i) {
            final WritableMap map = Arguments.createMap();
            map.putString("name", sNames[i]);
            map.putString("url", sImages[i]);
            photos.pushMap(map);
        }
        promise.resolve(photos);
        setBusy(false);
    }
    private void getPhotos(ReadableMap args, Promise promise) {
        String query;
        try {
            query = args.getString("tag");
        } catch (NoSuchKeyException e) {
            logger.error("getPhotos(): required property \"tag\" is missed in provided ReadableMap", e);
            promise.reject(ERROR_ILLEGAL_ARGUMENT, "Required property \"tag\" is missed");
            return;
        }

        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            final String q = query; final Promise p = promise;
            final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    MainModel.this.getPhotosImpl(q, p);
                    return null;
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            getPhotosImpl(query, promise);
        }
    }

    @Override
    public boolean canHandle(String method, ReadableMap args, Promise promise) {
        if ("getPhotos".equalsIgnoreCase(method)) {
            getPhotos(args, promise);
            return true;
        }

        return false;
        // create method helper to make runnable which calls promise from UI thread
        // or add such helper method to PromisingDelegateModule - that makes more sense actually...
        // hmph...
    }

    // cache response from client.hot() && client.search("tag"): List<TagPhotos> tagPhotos # Map<String, TagPhotos>? better keep ordered as in response from flickr
    // getTags() => (item.tag, item.photos[0].url) for item in tagPhotos
    // getPhotos(tag) => item.photos = tagPhotos.find(tag == item.tag)
    //                   (photo.user, photo.name, photo.url) for photo in item.photos
}
