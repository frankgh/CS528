package com.frankgh.wpiparking.services;

import android.app.IntentService;
import android.content.Intent;

import com.frankgh.wpiparking.DataRepository;
import com.frankgh.wpiparking.WPIParkingApplication;
import com.frankgh.wpiparking.db.entity.ActivityEntity;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * @author Vaseem
 */

public class ActivityRecognitionIntentService extends IntentService {

    private static final String TAG = "ActivityRecognitionIS";
    private static final String[] CODE_TEXT = {"IN_VEHICLE", "ON_BICYCLE", "ON_FOOT",
            "STILL", "UNKNOWN", "TILTING", "", "WALKING", "RUNNING"};

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    public ActivityRecognitionIntentService(String name) {
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

        switch (activity.getType()) {
            case DetectedActivity.STILL:
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.RUNNING:
            case DetectedActivity.WALKING:
            case DetectedActivity.IN_VEHICLE:
                DataRepository repo = ((WPIParkingApplication) getApplication()).getRepository();
                ActivityEntity entity = new ActivityEntity();
                entity.setTimestamp(System.currentTimeMillis());
                entity.setDetectedActivityId(activity.getType());
                entity.setDetectedActivityName(typeToText(activity.getType()));
                repo.insert(entity);
                break;
        }
    }

    private String typeToText(int code) {
        return CODE_TEXT[code];
    }
}
