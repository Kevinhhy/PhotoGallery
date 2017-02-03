package com.example.kevin.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by kevin on 2017/1/25.
 */

public class QueryPreferences {
    private static final String PRE_TAG = "searchQuery";
    private static final String PRE_LAST_RESULT_ID = "lastResultId";

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PRE_TAG, query)
                .apply();
    }

    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PRE_TAG, null);
    }

    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PRE_LAST_RESULT_ID, lastResultId)
                .apply();
    }

    public static String getPreLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PRE_LAST_RESULT_ID, null);
    }

}
