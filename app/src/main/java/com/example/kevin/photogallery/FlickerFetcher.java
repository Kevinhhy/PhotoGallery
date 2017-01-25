package com.example.kevin.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 2017/1/21.
 */

public class FlickerFetcher {

    private static final String TAG = "FlickerFetcher";

    private static final String API_KEY = "aff800ad6fda5acba630d364a6db9d4a";
    private static final String GETRECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";

    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("method", "flickr.photos.getRecent")
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSepc) throws IOException {
        return new String (getUrlBytes(urlSepc));
    }

    public List<GalleryItem> downloadGalleryItems(String url) {

        List<GalleryItem> list = new ArrayList<>();

        try {
            String jsonString = getUrlString(url);
            parseItems(list, jsonString);
            Log.i(TAG, "Recieved JSON: " + jsonString);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items: ", e);
        }

        return list;
    }

    private String buildUrl(String method, String query, int page) {
        Uri.Builder builder = ENDPOINT.buildUpon().appendQueryParameter("method", method);
        builder.appendQueryParameter("page", Integer.toString(page));
        if (method.equals(SEARCH_METHOD)) {
            builder.appendQueryParameter("text", query);
        }
        return builder.build().toString();
    }

    public List<GalleryItem> fetchPhoto(int page) {
        String url = buildUrl(GETRECENTS_METHOD, null, page);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhoto(String query, int page) {
        String url = buildUrl(SEARCH_METHOD, query, page);
        return downloadGalleryItems(url);
    }

    public void parseItems(List<GalleryItem> list, String  json) throws IOException{
        //利用Gson库的fromJson方法创建一个GsonData对象
        //fromJson方法第一个参数为需要解析的JSON，第二个对象为存储数据的类，即为GsonData类
        GsonData gsonData = new Gson().fromJson(json,GsonData.class );
        for (int i = 0; i < gsonData.getPhotos().getPerpage(); i++) {
            GalleryItem item = new GalleryItem();
            item.setCaption(gsonData.getPhotos().getPhoto().get(i).getTitle());
            item.setId(gsonData.getPhotos().getPhoto().get(i).getId());

            if (gsonData.getPhotos().getPhoto().get(i).getUrl_s() == null) {
                continue;
            }
            item.setUrl(gsonData.getPhotos().getPhoto().get(i).getUrl_s());
            list.add(item);
        }
    }
}
