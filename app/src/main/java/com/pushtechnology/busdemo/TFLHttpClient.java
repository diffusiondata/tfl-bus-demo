package com.pushtechnology.busdemo;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Created by dimeji on 22/04/16.
 */
public final class TFLHttpClient {

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler handler) {
        final AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, params, handler);
    }
}
