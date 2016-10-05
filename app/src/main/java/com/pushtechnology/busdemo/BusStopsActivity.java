package com.pushtechnology.busdemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.ErrorReason;
import com.pushtechnology.diffusion.client.features.Topics;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.session.SessionFactory;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;
import com.pushtechnology.diffusion.datatype.json.JSON;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class BusStopsActivity extends AppCompatActivity {

    private static final String URL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?Circle=";
    private static int RADIUS = 250;
    private static final String RETURN_LIST = "&ReturnList=StopID,StopPointName";
    private Topics topics;
    private final List<String> stops = new ArrayList<>();
    private final Map<String, Topics.ValueStream<JSON>> streamMap = new HashMap<>();
    private SessionHandler sessionHandler;
    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_stops);
        final String longlat = (String) getIntent().getExtras().get("location");

        query = URL + longlat + ',' + RADIUS + RETURN_LIST;

        if (sessionHandler == null) {
            sessionHandler = new SessionHandler();
            Diffusion.sessions().principal("admin").password("password").open(GPSSearchActivity.SERVER_URL, sessionHandler);
        }
    }

    @Override
    protected void onDestroy() {
        if ( sessionHandler != null ) {
            sessionHandler.close();
            sessionHandler = null;
        }
        super.onDestroy();
    }

    /**
     * A session handler that maintains the diffusion session.
     */
    private class SessionHandler implements SessionFactory.OpenCallback {

        private Session session;

        @Override
        public void onOpened(Session s) {
            session = s;
            Log.d("Diffusion Client", "Opened session with id: " + session.getSessionId().toString());

            TFLHttpClient.get(query, null, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    final String[] lines = new String(responseBody).split("\\r?\\n");

                    if (lines.length > 1) {

                        topics = session.feature(Topics.class);

                        final ListView list_view = ((ListView) findViewById(R.id.listView));

                        for (int i = 1; i < lines.length; i++) {
                            final String[] elements = lines[i].substring(1, lines[i].length() - 1).split(",");

                            final String stopName = elements[1].replaceAll("\"", "");

                            //StopId will always be the last element in the list from our response
                            final String stopId = elements[elements.length-1].replaceAll("\"", "");

                            if (!stopId.contains(" ")) {
                                stops.add( stopId + " - " + stopName );
                                final String topicPath = ">BusDemo/Stops/" + stopId;
                                topics.subscribe(topicPath, new SubscriptionCallback());
                            }
                        }

                        BusStopsActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                final ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, stops) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        TextView text = (TextView) view.findViewById(android.R.id.text1);
                                        text.setTextColor(Color.RED);
                                        return view;
                                    }
                                };

                                list_view.setAdapter(adapter);

                                list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        final String stopId = ((TextView) view).getText().toString().split("-")[0].trim();
                                        final String topicPath = ">BusDemo/Stops/" + stopId;

                                        final Topics.ValueStream<JSON> s = streamMap.get(stopId);

                                        if (s == null) {
                                            final Topics.ValueStream<JSON> stream = new BusStream(view);
                                            streamMap.put(stopId, stream);
                                            topics.addStream(topicPath, JSON.class, stream);
                                        }
                                        else {
                                            topics.removeStream(s);
                                            topics.addStream(topicPath, JSON.class, s);
                                        }

                                        view.setClickable(false);
                                    }
                                });
                            }
                        });
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d("HTTP Request", "HTTP request failed: " + statusCode);
                }

                @Override
                public boolean getUseSynchronousMode() {
                    return false;
                }
            });
        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e("Diffusion Client", "Failed to open session because: " + errorReason.toString());
            session = null;
        }

        public void close() {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private final class SubscriptionCallback implements Topics.CompletionCallback {

        @Override
        public void onComplete() {
            Log.d("Diffusion Client", "Subscription complete");
        }

        @Override
        public void onDiscard() {
            Log.d("Diffusion Client", "Subscription discarded");
        }
    }

    private final class BusStream implements Topics.ValueStream<JSON> {

        private final View view;

        public BusStream(View view) {
            this.view = view;
        }

        @Override
        public void onValue(final String topicPath, TopicSpecification topicSpecification, final JSON oldValue, final JSON newValue) {
            Log.i("Diffusion client", "onValue : " + topicPath);
            BusStopsActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final StringBuilder data = new StringBuilder();
                    Log.v("PATIENCE", newValue.toJsonString());

                    try {
                        final JSONObject dataJSON = new JSONObject(newValue.toJsonString());
                        final Iterator<String> keys = dataJSON.keys();

                        while (keys.hasNext()) {
                            final String key = keys.next();
                            data.append("Route Number: ").append(dataJSON.getJSONObject(key).get("RouteNumber"));
                            data.append("\n");
                            data.append("Destination: ").append(dataJSON.getJSONObject(key).get("Destination"));
                            data.append("\n");
                            data.append("Registration ID: ").append(dataJSON.getJSONObject(key).get("RegistrationID"));
                            data.append("\n");
                            data.append("Estimated Arrival Time: ").append(calculateArrival((String) dataJSON.getJSONObject(key).get("EstimatedArrivalTime"))).append(" min(s)");
                            data.append("\n");
                            data.append("\n");
                        }
                    }
                    catch (JSONException e) {
                        Log.e("Diffusion Client", "Unable to create JSON Object: " + e.getMessage());
                    }

                    ((TextView) view).setText(data.toString());
                    ((TextView) findViewById(R.id.lastUpdate)).setText("LAST UPDATED: " + new Date(System.currentTimeMillis()).toString());
                }
            });

        }

        @Override
        public void onSubscription(String s, TopicSpecification topicSpecification) {
            Log.i("Diffusion client", "Subscription succeeded: " + s);
        }

        @Override
        public void onUnsubscription(String s, TopicSpecification topicSpecification, Topics.UnsubscribeReason unsubscribeReason) {
            Log.i("Diffusion client", "Unsubscription succeeded: " + s);
        }

        @Override
        public void onClose() {
            Log.i("Diffusion client", "Stream closed");
        }

        @Override
        public void onError(ErrorReason errorReason) {
            Log.e("Diffusion client", "Error on stream: " + errorReason);
        }
    }

    private String calculateArrival(String time) {
        final String estimatedArrival = time.replaceAll("\"", "");
        final long millis = Long.parseLong(estimatedArrival);
        final long arrival = millis - System.currentTimeMillis();

        return Long.toString(TimeUnit.MILLISECONDS.toMinutes(arrival));
    }
}
