package com.frankgh.wpiparking;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vaseem
 */

public class ActivityRecognizedService extends IntentService {

    private static final String TAG = "ActivityRecognition";
    private static final String[] CODE_TEXT = {"IN_VEHICLE", "ON_BICYCLE", "ON_FOOT",
            "STILL", "UNKNOWN", "TILTING", "", "WALKING", "RUNNING"};

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result);
        }
    }

    private void handleDetectedActivities(ActivityRecognitionResult probableActivities) {
        DetectedActivity activity = probableActivities.getMostProbableActivity();

        if (activity.getType() == DetectedActivity.ON_FOOT ||
                activity.getType() == DetectedActivity.TILTING) {
            Set<Integer> s = new HashSet<>();
            s.add(DetectedActivity.RUNNING);
            s.add(DetectedActivity.WALKING);
            if (activity.getType() == DetectedActivity.TILTING)
                s.add(DetectedActivity.STILL);

            activity = null;
            for (DetectedActivity a : probableActivities.getProbableActivities()) {
                if (s.contains(a.getType()) &&
                        (activity == null || a.getConfidence() > activity.getConfidence())) {
                    activity = a;
                }
            }
        }

        if (activity == null)
            return;

        switch (activity.getType()) {
            case DetectedActivity.STILL:
            case DetectedActivity.RUNNING:
            case DetectedActivity.WALKING: {
                Log.d(TAG, typeToText(activity.getType()) + "(" + activity.getType() + "): " + activity.getConfidence());
                Intent intent = new Intent("ActivityRecognizedService#ActivityChange");
                intent.putExtra("currentActivityCode", activity.getType());
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            }
            default:
                break;
        }
    }

    private String typeToText(int code) {
        return CODE_TEXT[code];
    }
}