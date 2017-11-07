package com.example.vaseem.activityrecognition;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    public GoogleApiClient mClientAPI;
    public static  ImageView activityImage;
    public static TextView activityText;
    PendingIntent pendingIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityText= (TextView) findViewById(R.id.activityText);
        activityImage= (ImageView) findViewById(R.id.activityImage);

        mClientAPI=  new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClientAPI.connect();


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(this,ActivityRecognisingService.class);
        pendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClientAPI,250,pendingIntent);

    }

    @Override
    protected void onStop() {
        super.onStop();
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClientAPI,pendingIntent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClientAPI,250,pendingIntent);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static void setImageDrawable(int activity_image) {
        activityImage.setImageResource(activity_image);

    }

    public static void setText(String activity_text) {
        activityText.setText(activity_text);
    }


}
