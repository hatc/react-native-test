package com.basictest.flickr;

public class FlickrApiError extends Exception {
    public FlickrApiError(String message) { super(message);  }
    public FlickrApiError(String message, Throwable cause) { super(message, cause); }
    public FlickrApiError(String prefix, int code, String message) { super(getErrorMessage(prefix, code, message)); }

    static final String sFlickrApiError = "Flickr API error code ";
    static final String sHttpError = "HTTP GET status code ";
    static String getErrorMessage(String prefix, int code, String message) {
        return (new StringBuilder())
                .append(prefix)
                .append(code)
                .append(": ")
                .append(message)
                .toString();
    }
}
