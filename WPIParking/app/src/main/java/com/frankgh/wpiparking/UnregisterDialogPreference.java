package com.frankgh.wpiparking;

import android.content.Context;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/10/17.
 */

public class UnregisterDialogPreference extends DialogPreference {

    private static final String TAG = "UnregisterDP";

    /**
     * Provides the authenticated user for Firebase.
     */
    protected FirebaseAuth mAuth;

    public UnregisterDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return;
        }
//        revokeAccess();
    }

    /**
     * Revoke access to Google provider.
     */
    private void revokeAccess() {
        FirebaseUser user = mAuth.getCurrentUser();
        List<? extends UserInfo> data = user.getProviderData();
        for (UserInfo info : data) {
            Log.d(TAG, "Logging out Provider: " + info.getProviderId() + " ....");
            if (FirebaseAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                // Sign out from Firebase
                mAuth.signOut();
            } else if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getContext().getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
                // Google revoke access
                mGoogleSignInClient.revokeAccess().addOnCompleteListener((Executor) getContext(),
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
//                                showLoginChooser();
                            }
                        });
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            revokeAccess();
        }
    }
}
