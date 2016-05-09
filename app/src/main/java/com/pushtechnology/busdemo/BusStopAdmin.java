package com.pushtechnology.busdemo;

import android.util.Log;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.features.RegisteredHandler;
import com.pushtechnology.diffusion.client.features.control.topics.TopicAddFailReason;
import com.pushtechnology.diffusion.client.features.control.topics.TopicControl;
import com.pushtechnology.diffusion.client.features.control.topics.TopicUpdateControl;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicType;
import com.pushtechnology.diffusion.datatype.json.JSON;

/**
 * Created by dimeji on 24/04/16.
 */
public class BusStopAdmin {

    private final Session session;
    private final TopicControl topicControl;
    private final TopicUpdateControl topicUpdateControl;
    private final TopicUpdateControl.ValueUpdater valueUpdater;

    public BusStopAdmin() {
        session = Diffusion.sessions().principal("admin").password("password").open("ws://192.168.53.66:8080");
        topicControl = session.feature(TopicControl.class);
        topicUpdateControl = session.feature(TopicUpdateControl.class);

        valueUpdater = topicUpdateControl.updater().valueUpdater(JSON.class);

        topicControl.addMissingTopicHandler("BusDemo/Stops", new MissingTopicCallback());
    }

    private final class MissingTopicCallback implements TopicControl.MissingTopicHandler {

        @Override
        public void onMissingTopic(TopicControl.MissingTopicNotification missingTopicNotification) {
            final String topicPath = missingTopicNotification.getTopicPath();
            topicControl.addTopic(topicPath, TopicType.JSON, new TopicCallback(missingTopicNotification));
        }

        @Override
        public void onActive(String s, RegisteredHandler registeredHandler) {
            Log.i("Diffusion Admin", "Missing topic handler active");
        }

        @Override
        public void onClose(String s) {
            Log.i("Diffusion Admin", "Missing topic handler closed");
        }
    }

    private final class TopicCallback implements TopicControl.AddCallback {

        private final TopicControl.MissingTopicNotification mtn;

        public TopicCallback(TopicControl.MissingTopicNotification mtn) {
            this.mtn = mtn;
        }

        @Override
        public void onTopicAdded(String topicPath) {
            Log.d("Diffusion Admin", "Topic Added: " + topicPath);
            mtn.proceed();

            new PollingTFLDataGrabber(valueUpdater, topicPath).start();
        }

        @Override
        public void onTopicAddFailed(String s, TopicAddFailReason topicAddFailReason) {
            Log.d("Diffusion Admin", "Topic Add failed: " + topicAddFailReason);
            mtn.cancel();
        }

        @Override
        public void onDiscard() {
            Log.d("Diffusion Admin", "Topic Add discarded");
        }
    }
}
