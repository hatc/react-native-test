package com.basictest.flickr;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class TagPhotos {
    public static final class Photo {
        public Photo(String url, String title, String username) {
            this.url = url; this.title = title; this.username = username;
        }
        public Photo(JSONObject json) throws FlickrApiError {
        }

        private String url, title, username;
        public String url() { return url; }
        public String title() { return title; }
        public String username() { return username; }
    }

    private String tag;
    public String tag() { return tag; }
    private ArrayList<Photo> photos;
    public List<Photo> photos() {
        if (photos == null)
            photos = new ArrayList<>();
        return photos;
    }
    public boolean isEmpty() { return photos == null || photos.isEmpty(); }

    public void addPhoto(String url, String title, String username) {
        photos().add(new Photo(url, title, username));
    }
    public void addPhoto(JSONObject json) throws FlickrApiError {
        photos().add(new Photo(json));
    }

    public TagPhotos(String tag) {
        this.tag = tag;
    }
    public TagPhotos(JSONObject json) throws FlickrApiError {
        this.tag = FlickrJSONResponse.getString(json, "_content");
    }
}
