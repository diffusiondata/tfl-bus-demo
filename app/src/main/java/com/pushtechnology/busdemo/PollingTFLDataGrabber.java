package com.pushtechnology.busdemo;

/**
 * Created by dimeji on 25/04/16.
 */

import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.datatype.json.JSON;
import com.pushtechnology.diffusion.datatype.json.JSONDataType;
import com.pushtechnology.diffusion.datatype.json.JSONDelta;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

/**
 * Created by dimeji on 22/04/16.
 */
public final class PollingTFLDataGrabber implements DataGrabber {

    private final String topicPath;
    private final TopicUpdateControl.ValueUpdater<JSON> updater;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JSONDataType dataType = Diffusion.dataTypes().json();

    private final static String QUERY = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?StopID=";
    private final static String RETURN_LIST = "&ReturnList=RegistrationNumber,LineName,DestinationName,EstimatedTime";

    public PollingTFLDataGrabber(TopicUpdateControl.ValueUpdater<JSON> updater,  String topicPath) {
        this.topicPath = topicPath;
        this.updater = updater;
    }

    @Override
    public void start() {
        final String[] topicParts = topicPath.split("/");
        final String url = QUERY + topicParts[topicParts.length-1] + RETURN_LIST;

        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                TFLHttpClient.get(url, null, new AsyncHttpResponseHandler(Looper.getMainLooper()) {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        final String[] dataLines = new String(responseBody).split("\\r?\\n");

                        final StringBuilder jsonString = new StringBuilder();

                        if (dataLines.length > 1) {

                            jsonString.append('{');

                            for (int i = 1; i < dataLines.length; i++) {
                                final String line = dataLines[i].substring(1, dataLines[i].length() - 1);
                                final String[] data = line.split(",");
                                final JSONObject bus = new JSONObject();
                                final JSONObject busObject = new JSONObject();

                                try {
                                    bus.put("RouteNumber", data[1]);
                                    bus.put("Destination", data[2]);
                                    bus.put("RegistrationID", data[3]);
                                    bus.put("EstimatedArrivalTime", data[4]);

                                    busObject.put(Integer.toString(i), bus);
                                }
                                catch (JSONException e) {
                                    Log.e("HTTP client", "Error in creating JSON object");
                                }

                                final String jsonData = busObject.toString().substring(1, busObject.toString().length() - 1);

                                jsonString.append(jsonData);

                                if (i != dataLines.length - 1) {
                                    jsonString.append(',');
                                }
                                else {
                                    jsonString.append('}');
                                }
                            }

                            final JSON oldValue = updater.getCachedValue(topicPath);
                            final JSON newValue = dataType.fromJsonString(jsonString.toString());

                            // Old value could be null if this is the first update performed
                            // on the topic.
                            if (oldValue != null) {
                                final JSONDelta delta = oldValue.diff(newValue);
                                if (delta.hasChanges()) {
                                    updater.update(topicPath, newValue, new UpdateNotifier());
                                }
                            }
                            else {
                                updater.update(topicPath, newValue, new UpdateNotifier());
                            }
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e("HTTP client", "HTTP request failed: " + statusCode);
                    }
                });
            }

        }, 0, 30, TimeUnit.SECONDS);

    }

    private final class UpdateNotifier implements TopicUpdateControl.Updater.UpdateCallback {

        @Override
        public void onSuccess() {
            Log.v("TFL Data Grabber", "Topic Update success");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e("TFL Data Grabber", "Error: " + errorReason);
        }
    }
}