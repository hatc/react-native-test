package com.basictest.flickr;

import org.json.JSONObject; // jackson? nah, too complex ^_^
import org.json.JSONException;

final class FlickrJSONResponse {
    static String getString(JSONObject json, String key) throws FlickrApiError {
        try { return json.getString(key); }
        catch (JSONException e) { throw new FlickrApiError("unexpected JSON format in response", e); }
    }

    static JSONObject readFlickrApiResponse(String json, String root) throws FlickrApiError {
        /*
        "stat"    : "fail",
        "code"    : "97",
        "message" : "Missing signature"
        ||
        "stat"    : "ok?",*/
        try {
            JSONObject r = new JSONObject(json);
            if ("fail".equals(r.getString("stat")))
                throw new FlickrApiError(FlickrApiError.sFlickrApiError,
                        r.optInt("code", -1),
                        r.optString("message", ""));

            r = r.optJSONObject(root);
            if (r == null)
                throw new FlickrApiError(String.format("property \"%s\" not found in json response", root));
            return r;
        } catch (JSONException e) {
            throw new FlickrApiError("unexpected JSON format in response", e);
        }
    }
}
