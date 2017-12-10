package com.frankgh.wpiparking;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MakeNoteActivity extends BaseActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    /**
     * The path to the note images
     */
    private static final String NOTE_IMAGE_PATH = "noteImages";

    /**
     * The name of the image filename
     */
    private static final String FILENAME = "image.jpg";

    /**
     * The name of the reference for nodes
     */
    private static final String NOTES_REF_NAME = "notes";

    @BindView(R.id.buttonUpload)
    Button buttonUpload;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.editText)
    EditText editText;
    /**
     * The database reference
     */
    private DatabaseReference mNotesReference;

    /**
     * The storage references for NOTE_IMAGE_PATH
     */
    private StorageReference mImagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        ButterKnife.bind(this);

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        mNotesReference = databaseReference.child(NOTES_REF_NAME).child(getUid());
        mNotesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                editText.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        // Create a child reference
        // imagesRef now points to "noteImages"
        StorageReference parentRef = storageRef.child(NOTE_IMAGE_PATH);

        // The reference to the image
        mImagesRef = parentRef.child(getUid()).child(FILENAME);

        mImagesRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'noteImages/uid/profile.png'
                // Pass it to Picasso to download, show in ImageView and caching
                Picasso
                        .with(getApplicationContext())
                        .load(uri.toString())
                        .fit()
                        .centerInside()
                        .into(imageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //handling the image chooser activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
    }

    @OnClick({R.id.imageView, R.id.buttonUpload})
    public void onClick(View view) {
        if (view == imageView) {
            dispatchTakePictureIntent();
        } else if (view == buttonUpload) {
            uploadFile();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void uploadFile() {
        showProgressDialog(getString(R.string.uploading));

        // Save note
        mNotesReference.setValue(editText.getText().toString());
        mNotesReference.push();

        // Get the data from an ImageView as bytes
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = imageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Create file metadata including the content type
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .build();

        // Save the image
        UploadTask uploadTask = mImagesRef.putBytes(data, metadata);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                hideProgressDialog();
                finish();
            }
        });
    }
}
