package com.wpics528.android.criminalintent;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
public class CrimeImageGalleryActivity extends AppCompatActivity {

    @BindView(R.id.crimeImage_recycler_view)
    RecyclerView mCrimeImageRecyclerView;
    private File mStorageFile;
    private Crime mCrime;
    private List<String> mPhotoFileList;
    private CrimeImageListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_image_gallery);
        ButterKnife.bind(this);

        UUID crimeId = (UUID) getIntent().getSerializableExtra(CrimePagerActivity.EXTRA_CRIME_ID);
        mCrime = CrimeLab.get(this).getCrime(crimeId);
        mStorageFile = CrimeLab.get(this).getStorageDir(mCrime);
        mPhotoFileList = CrimeLab.get(this).getPhotoFileList(mCrime);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.camera_string);
        getSupportActionBar().setSubtitle(getString(R.string.photos_string, mCrime.getPhotoCount()));

        if (mCrime.getPhotoCount() < 16) {
            mCrimeImageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mCrimeImageRecyclerView.setItemViewCacheSize(4);
        } else {
            mCrimeImageRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        }

        mCrimeImageRecyclerView.setHasFixedSize(true);
        mCrimeImageRecyclerView.setDrawingCacheEnabled(true);
        mCrimeImageRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        mAdapter = new CrimeImageListAdapter(this, mPhotoFileList);
        mCrimeImageRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class CrimeImageListHolder extends RecyclerView.ViewHolder {

        public ImageView crimeImageView;
        private Activity mActivity;

        public CrimeImageListHolder(Activity activity, View itemView) {
            super(itemView);
            mActivity = activity;
            crimeImageView = itemView.findViewById(R.id.list_item_crime_Image_crimeImageView);
        }
    }

    private class CrimeImageListAdapter extends RecyclerView.Adapter<CrimeImageListHolder> {

        private Activity mActivity;
        private List<String> mCrimePics;

        public CrimeImageListAdapter(Activity activity, List<String> crimePics) {
            mActivity = activity;
            mCrimePics = crimePics;
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public CrimeImageListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
            View view = layoutInflater
                    .inflate(R.layout.list_item_crime_image, parent, false);
            return new CrimeImageListHolder(mActivity, view);
        }

        @Override
        public void onBindViewHolder(final CrimeImageListHolder holder, final int position) {
            final String currentPhotoPath = "file:" + mStorageFile.getAbsolutePath();
            final String photoFilename = mCrimePics.get(position);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable=true;
            holder.crimeImageView.buildDrawingCache();
            Bitmap myBitmap = holder.crimeImageView.getDrawingCache();
            Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
            Canvas tempCanvas = new Canvas(tempBitmap);
            tempCanvas.drawBitmap(myBitmap, 0, 0, null);
            FaceDetector faceDetector = new
                    FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                    .build();
            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Face> faces = faceDetector.detect(frame);
            if (faces.size() > 0 && mCrime.isFaceDetectionEnabled()){
                holder.crimeImageView.setAlpha((float)0.3);
            }
            holder.crimeImageView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        // Wait until layout to call Picasso
                        @Override
                        public void onGlobalLayout() {
                            // Ensure we call this only once
                            holder.crimeImageView.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            int pos = getItemViewType(position);
                            if(mCrimePics.get(pos) == null) {

                                holder.crimeImageView.setVisibility(View.GONE);
                            } else {

                            Picasso.with(mActivity)
                                    .load(currentPhotoPath + "/" + photoFilename)
                                    .resize(holder.crimeImageView.getWidth(), 0)
                                    .into(holder.crimeImageView, new Callback() {
                                        @Override
                                        public void onSuccess() {

                                            if (!mCrime.isFaceDetectionEnabled()){
                                                return;
                                            }

                                            Bitmap myBitmap = ((BitmapDrawable) holder.crimeImageView.getDrawable()).getBitmap();

                                            Paint myRectPaint = new Paint();
                                            myRectPaint.setStrokeWidth(5);
                                            myRectPaint.setColor(Color.RED);
                                            myRectPaint.setStyle(Paint.Style.STROKE);

                                            Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
                                            Canvas tempCanvas = new Canvas(tempBitmap);
                                            tempCanvas.drawBitmap(myBitmap, 0, 0, null);

                                            FaceDetector faceDetector = new
                                                    FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                                                    .build();
                                            if (!faceDetector.isOperational()) {

                                                return;
                                            }

                                            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
                                            SparseArray<Face> faces = faceDetector.detect(frame);

                                            if (faces.size() == 0) return;

                                            for (int i = 0; i < faces.size(); i++) {
                                                Face thisFace = faces.valueAt(i);
                                                float x1 = thisFace.getPosition().x;
                                                float y1 = thisFace.getPosition().y;
                                                float x2 = x1 + thisFace.getWidth();
                                                float y2 = y1 + thisFace.getHeight();
                                                tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
                                            }
                                            holder.crimeImageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

                                        }

                                        @Override
                                        public void onError() {
                                        }
                                    });}

                        }
                    });
        }

        @Override
        public int getItemCount() {
            return mCrimePics.size();
        }
    }
}
