package com.basictest.flickr;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlickrClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(FlickrClient.class);
    private final Object lock = new Object();

    private OkHttpClient httpClient;
    private OkHttpClient getHttpClient() {
        synchronized (lock) {
            if (httpClient == null) {
                httpClient = (new OkHttpClient.Builder())
                        .cookieJar(cookieJar)
                        .build();
            }
            return httpClient;
        }
    }

    private static final String sFlickrApiUrl = "https://api.flickr.com/services/rest/";
    private String flickrApiKey;
    public void setFlickrApiKey(String v) {
        if (v == null || v.length() < 1)
            throw new IllegalArgumentException("flickrApiKey must be non empty string");

        flickrApiKey = v;
    }
    private HttpUrl getFlickrApiUrl(String[] keys, String[] values) {
        // ?method=flickr.photos.search&api_key=1934408aa94323d57ef7793ead960aa2&format=json&nojsoncallback=1";
        if (keys.length != values.length)
            throw new IllegalArgumentException("keys.length != values.length");
        if (flickrApiKey == null)
            throw new IllegalStateException("flickrApiKey is null, call setFlickrApiKey() first");

        final HttpUrl.Builder builder = HttpUrl.parse(sFlickrApiUrl).newBuilder();
        for (int i = 0; i < keys.length; ++i)
            builder.addQueryParameter(keys[i], values[i]);

        return builder
                .addQueryParameter("api_key", flickrApiKey)
                .addQueryParameter("format", "json")
                .addQueryParameter("nojsoncallback", "1")
                .build();
    }

    // use getHot to get just first tag, i.e tag with score max
    private String getHotTag() throws FlickrApiError {
        String[] keys = {
                "method"
        };
        String[] values = {
                "flickr.tags.getHotList"
        };

        JSONObject response = getFlickrApiResponse(getFlickrApiUrl(keys, values), "hottags");
        try {
            JSONArray tags = response.getJSONArray("tag");
            TreeMap<Integer, JSONObject> tagsMap = new TreeMap<>();
            for (int i = 0; i < tags.length(); ++i) {
                JSONObject tag = tags.getJSONObject(i);
                tagsMap.put(tag.getInt("score"), tag);
            }

            return tagsMap.lastEntry().getValue().getString("_content");
        } catch (JSONException e) {
            throw new FlickrApiError("unexpected JSON format in response", e);
        }
    }
    public List<TagPhotos> hot() throws FlickrApiError {
        String tag = getHotTag();
        yield();
        return getTagPhotos(tag);
    }

    // public static final int sMaxPhotosPerTag = 0x100;
    private List<TagPhotos> getTagPhotos(String tag) throws FlickrApiError {
        try {
            JSONArray tags;
            {
                String[] keys = {
                        "method",
                        "tag"
                };
                String[] values = {
                        "flickr.tags.getClusters",
                        tag
                };
                JSONObject response = getFlickrApiResponse(getFlickrApiUrl(keys, values), "clusters");
                tags = response.getJSONArray("cluster");
            }

            ArrayList<TagPhotos> r = new ArrayList<>();
            for (int i = 0; i < tags.length(); ++i) {
                JSONArray t = tags.getJSONObject(i).getJSONArray("tag");
                if (t.length() < 1)
                    continue;

                TagPhotos tagPhotos = new TagPhotos(t.getJSONObject(0));
                StringBuilder id = new StringBuilder(tagPhotos.tag());
                for (int j = 1, n = Math.min(3, t.length()); j < n; ++j) {
                    id.append(t.getJSONObject(j).getString("_content"));
                    if (j + 1 < n)
                        id.append(',');
                }

                String[] keys = {
                        "method",
                        "tag",
                        "cluster_id"
                };
                String[] values = {
                        "flickr.tags.getClusterPhotos",
                        tag,
                        id.toString()
                };
                JSONObject response = getFlickrApiResponse(getFlickrApiUrl(keys, values), "photos");
                JSONArray photos = response.getJSONArray("photo");

                for (int j = 0; j < photos.length(); ++j) {
                    JSONObject photo = photos.getJSONObject(j);
                    if (photo.getInt("ispublic") != 1)
                        continue;

                    tagPhotos.addPhoto(photo);
                }
                if (!tagPhotos.isEmpty())
                    r.add(tagPhotos);
            }
            return r;
        } catch (JSONException e) {
            throw new FlickrApiError("unexpected JSON format in response", e);
        }
    }

    public List<TagPhotos> search(String tag) throws FlickrApiError {
        try {
            String[] keys = {
                    "method",
                    "tag"
            };
            String[] values = {
                    "flickr.tags.getRelated",
                    tag
            };
            JSONObject response = getFlickrApiResponse(getFlickrApiUrl(keys, values), "tags");
            JSONArray tags = response.getJSONArray("tag");
            if (tags.length() < 1)
                return Collections.emptyList();

            return getTagPhotos(tags.getJSONObject(0).getString("_content"));
        } catch (JSONException e) {
            throw new FlickrApiError("unexpected JSON format in response", e);
        }
    }

    private JSONObject getFlickrApiResponse(HttpUrl url, String root) throws FlickrApiError {
        cookieJar.clearAllCookies();
        Request request = createBuilderForKindaBrowser(url)
                .build();
        try {
            final Response response = getHttpClient().newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.error("getFlickrApiResponse(): GET status code {}: {}",
                        response.code(), response.message());
                throw new FlickrApiError(FlickrApiError.sHttpError, response.code(), response.message());
            }
            try (ResponseBody responseBody = response.body()) {
                return FlickrJSONResponse.readFlickrApiResponse(responseBody.string(), root);
            }
        } catch (IOException e) {
            logger.error("getFlickrApiResponse() failed", e);
            throw new FlickrApiError("getFlickrApiResponse() failed", e);
        }
    }

    private class StraightforwardCookieJar implements CookieJar {
        HashMap<String, List<Cookie>> host2cookies = new HashMap<>();
        private List<Cookie> getOrDefault(String host, List<Cookie> defaultValue) {
            List<Cookie> r = host2cookies.get(host);
            return r == null ? defaultValue : r;
        }

        /**
         * Saves {@code cookies} from an HTTP response to this store according to this jar's policy.
         * <p>
         * <p>Note that this method may be called a second time for a single HTTP response if the response
         * includes a trailer. For this obscure HTTP feature, {@code cookies} contains only the trailer's
         * cookies.
         *
         * @param url
         * @param cookies
         */
        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            if (url == null) // Objects.requireNonNull() in API 19...
                throw new NullPointerException("url == null");
            if (cookies == null)
                throw new NullPointerException("cookies == null");

            synchronized (lock) {
                List<Cookie> l = getOrDefault(url.host(), new ArrayList<Cookie>(cookies.size()));
                if (!l.isEmpty()) {
                    for (Cookie cookie : cookies)
                        setCookie(l, cookie);
                } else {
                    l.addAll(cookies);
                }
            }

        }

        /**
         * Load cookies from the jar for an HTTP request to {@code url}. This method returns a possibly
         * empty list of cookies for the network request.
         * <p>
         * <p>Simple implementations will return the accepted cookies that have not yet expired and that
         * {@linkplain Cookie#matches match} {@code url}.
         *
         * @param url
         */
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            synchronized (lock) {
                return getOrDefault(url.host(), java.util.Collections.<Cookie>emptyList());
            }
        }

        void setCookie(String name, String value) {
            final Cookie cookie = (new Cookie.Builder())
                    .name(name)
                    .value(value)
                    .build();

            synchronized (lock) {
                for (HashMap.Entry<String, List<Cookie>> entry : host2cookies.entrySet())
                    setCookie(entry.getValue(), cookie);
            }
        }

        void clearAllCookies() {
            synchronized (lock) {
                host2cookies.clear();
            }
        }

        private void setCookie(List<Cookie> cookies, Cookie cookie) {
            for (int i = 0; i < cookies.size(); ++i) {
                if (cookies.get(i).name().equals(cookie.name())) {
                    cookies.set(i, cookie);
                    return;
                }
            }
            cookies.add(cookie);
        }
    }
    private final StraightforwardCookieJar cookieJar = new StraightforwardCookieJar();

    private Request.Builder createBuilderForKindaBrowser(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                // .addHeader("accept-encoding", "gzip") // don't add manually or decode byteStream() manually
                .addHeader("accept-language", "en,en-US;q=0.8,ru;q=0.6")
                .addHeader("upgrade-insecure-requests", "1")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
    }

    private static final String[] sFlickrApiExploreUrls = {
            "https://www.flickr.com/services/api/explore/flickr.tags.getRelated",
            "https://www.flickr.com/services/api/explore/flickr.tags.getHotList",
            "https://www.flickr.com/services/api/explore/flickr.tags.getClusters",
            "https://www.flickr.com/services/api/explore/flickr.tags.getClusterPhotos"
    };
    private final Random rnd = new Random();
    private int randint(int a, int b) { return a + rnd.nextInt(b - a + 1); }
    private static String findAndGetWithRegex(final String s, final String find, final Pattern regex) {
        final int head = s.indexOf(find);
        if (head < 0)
            return null;
        final Matcher matcher = regex.matcher(s);
        if (!matcher.find(head))
            return null;
        return matcher.group(1);
    }
    private void yield() {
        try { Thread.sleep(randint(1, 20)); }
        catch (InterruptedException ignored) {}
    }
    public String tryAcquireFlickrApiKey() {
        final HttpUrl url = HttpUrl.parse(sFlickrApiExploreUrls[randint(0, sFlickrApiExploreUrls.length - 1)]);

        cookieJar.clearAllCookies();
        Request request = createBuilderForKindaBrowser(url)
                .build();
        String magicCookie;
        try {
            final Response response = getHttpClient().newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.error("acquireFlickrApiKey(): GET status code {}: {}",
                        response.code(), response.message());
                return null;
            }
            try (ResponseBody responseBody = response.body()) {
                final String s = responseBody.string();
                magicCookie = findAndGetWithRegex(s/*responseBody.string()*/, "magic_cookie", Pattern.compile("=\"(\\w+)\""));
                if (magicCookie == null) {
                    logger.error("acquireFlickrApiKey(): unexpected response body, magic_cookie not found");
                    try (FileWriter out = new FileWriter("C:\\src\\android\\projects\\response.html")) {
                        out.write(s);
                    }
                    return null;
                }
            }
        } catch (IOException e) {
            logger.error("acquireFlickrApiKey(): GET failed", e);
            return null;
        }

        cookieJar.setCookie("liqpw", Integer.toString(randint(1280,1920)));
        cookieJar.setCookie("liqph", Integer.toString(randint(720,1080)));
        final RequestBody requestBody = (new FormBody.Builder())
                .add("method", url.pathSegments().get(url.pathSegments().size() - 1))
                .add("magic_cookie", magicCookie)
                .add("param_period", "")
                .add("param_count", "")
                .add("format", "json-nc")
                .add("sign_call", "none")
                .build();
        request = createBuilderForKindaBrowser(url)
                .post(requestBody)
                .build();
        String r;
        try {
            yield();
            final Response response = getHttpClient().newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.error("acquireFlickrApiKey(): POST status code {}: {}",
                        response.code(), response.message());
                return null;
            }
            try (ResponseBody responseBody = response.body()) {
                r = findAndGetWithRegex(responseBody.string(), "/services/api/render", Pattern.compile("api_key=(\\w+)"));
                if (r == null)
                    logger.error("acquireFlickrApiKey(): unexpected response body, api_key not found");
            }

        } catch (IOException e) {
            logger.error("acquireFlickrApiKey(): POST failed", e);
            return null;
        }
        return r;
    }

    // HttpUrl url = new HttpUrl.Builder()
    // .scheme("https")
    // .host("www.google.com")
    // .addPathSegment("search")
    // .addQueryParameter("q", "polar bears")
    // ... .addQueryParameter(k, v) - might utilize String[] with function arguments )))
    //                                i.e. getHot(tag, ) { String[] = { API_KEY, tag }; String[] = { "api_key", "tag" } for int i = 0 ))) } actually it' even new method do(String[] keys, String[] values) if keys.length !+ values.length throw ArgumentException
    // .build();

    // search input - limit to 0x100 chars on Android side, i.e. in EditBox
    // add drawable with search icon and hint Find a photo , android:drawableLeft="@drawable/search", android:drawableRight="@drawable/cancel"

    // FlickrApiError : Exception

    /**
     * Closes OkHttpClient and release associated resources.
     */
    @Override
    public void close() {
        synchronized (lock) {
            if (httpClient == null)
                return;

            try { httpClient.dispatcher().executorService().shutdown(); }
            catch (SecurityException ignored) {}

            httpClient.connectionPool().evictAll();
            if (httpClient.cache() != null) {
                try { httpClient.cache().close(); }
                catch (IOException e) { logger.error("httpClient.cache().close() throws an error", e); }
            }
            httpClient = null;
        }
    }
}
