package com.example.vaseem.activityrecognition;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.sql.Time;
import java.util.List;

public class ActivityRecognisingService extends IntentService {

    ActivityRecognitionResult result;
    private static String  oldActivityType;
    public static  String currentActivity;
    public static Long time;

    public ActivityRecognisingService() {
        super("ActivityRecognisingService");

    }



    public void updateUI(String message, int resource, String activity, long timeT) {
        final String msg = message;
        final int resId = resource;
        final String oldActivity = activity;
        final long time = timeT;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String sec = Long.toString((time/1000)%60);
                String min = Long.toString((time/60000));
                Toast toast = Toast.makeText(getApplicationContext(), oldActivity + " for  " + min + " min "+sec+" sec", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER,0,0);
                toast.show();
                MainActivity.setText("You Are "+msg);
                MainActivity.setImageDrawable(resId);
            }
        });
    }

    public void handleActivity(int resId){

        if(oldActivityType == null)
        {

            updateUI(currentActivity, resId,"Just Started",(long) 0);
            time = result.getElapsedRealtimeMillis();
            oldActivityType=currentActivity;
        }
        else{
            if(oldActivityType != currentActivity){
                long temp = result.getElapsedRealtimeMillis();
                long elapsed = temp-time;
                updateUI(currentActivity, resId,"You Were "+oldActivityType,elapsed);
                time= result.getElapsedRealtimeMillis();
                oldActivityType=currentActivity;
            }}

    }




    protected void onHandleIntent(Intent intent) {

        if(ActivityRecognitionResult.hasResult(intent)){
            result= ActivityRecognitionResult.extractResult(intent);
            DetectedActivity  activity= result.getMostProbableActivity();



            Log.e( "ActivityRecogition", "Activity: " + activity.getType() );

            switch (activity.getType()){
               case DetectedActivity.STILL: {
                   currentActivity="Still";
                   Log.e( "ActivityRecogition", "Still: " + activity.getConfidence() );
                  handleActivity(R.drawable.still);

                    break;
                }

                case DetectedActivity.WALKING: {
                    currentActivity="Walking";
                    Log.e( "ActivityRecogition", "Walking: " + activity.getConfidence() );
                    handleActivity(R.drawable.walking);

                    break;
                }

                case DetectedActivity.RUNNING: {
                    currentActivity="Running";
                    handleActivity(R.drawable.running);

                    Log.e( "ActivityRecogition", "Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT:{
                    DetectedActivity betterActivity = walkingOrRunning(result.getProbableActivities());
                    if (null != betterActivity) {
                        switch (betterActivity.getType()) {
                            case DetectedActivity.WALKING: {
                                currentActivity="Walking";
                                Log.e("ActivityRecogition", "Walking: " + activity.getConfidence());
                                handleActivity(R.drawable.walking);

                                break;

                            }
                            case DetectedActivity.RUNNING: {
                                currentActivity="Running";
                                handleActivity(R.drawable.running);

                                Log.e("ActivityRecogition", "Running: " + activity.getConfidence());
                                break;
                            }
                        }
                    }
                }
                case DetectedActivity.TILTING: {
                    DetectedActivity betterActivity = walkingOrRunningorStill(result.getProbableActivities());
                    if (null != betterActivity) {
                        switch (betterActivity.getType()) {
                            case DetectedActivity.STILL: {
                                currentActivity="Still";
                                Log.e( "ActivityRecogition", "Still: " + activity.getConfidence() );
                                handleActivity(R.drawable.still);

                                break;
                            }
                            case DetectedActivity.WALKING: {
                                currentActivity="Walking";
                                Log.e("ActivityRecogition", "Walking: " + activity.getConfidence());
                                handleActivity(R.drawable.walking);
                                break;

                            }
                            case DetectedActivity.RUNNING: {
                                currentActivity="Running";
                                handleActivity(R.drawable.running);

                                Log.e("ActivityRecogition", "Running: " + activity.getConfidence());
                                break;
                            }
                        }
                    }
                }



            }


        }

    }

    private DetectedActivity walkingOrRunning(List<DetectedActivity> probableActivities) {
        DetectedActivity myActivity = null;
        int confidence = 0;
        for (DetectedActivity activity : probableActivities) {
           if (activity.getType() != DetectedActivity.RUNNING && activity.getType() != DetectedActivity.WALKING)
                continue;

            if (activity.getConfidence() > confidence)
                myActivity = activity;


        }

        return myActivity;
    }

    private DetectedActivity walkingOrRunningorStill(List<DetectedActivity> probableActivities) {
        DetectedActivity myActivity = null;
        int confidence = 0;
        for (DetectedActivity activity : probableActivities) {

           if (activity.getType()== DetectedActivity.WALKING || activity.getType()== DetectedActivity.RUNNING || activity.getType()==DetectedActivity.STILL){
               if(activity.getConfidence()>confidence){
                   myActivity= activity;
               }
           }
           else
               continue;
        }

        return myActivity;
    }



}
