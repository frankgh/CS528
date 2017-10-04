package com.wpics528.android.criminalintent;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CrimeImageGalleryActivity extends AppCompatActivity {

    @BindView(R.id.crimeImage_recycler_view)
    RecyclerView mCrimeImageRecyclerView;
    private File mPhotoFile;
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
        mPhotoFileList = CrimeLab.get(this).getPhotoFileList(mCrime);
        String subtitle = Integer.toString(mPhotoFileList.size())+" "+ getString(R.string.photos_string);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.camera_string);
        getSupportActionBar().setSubtitle(subtitle);
        mCrimeImageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mCrimeImageRecyclerView.setHasFixedSize(true);
        mCrimeImageRecyclerView.setItemViewCacheSize(4);
        mCrimeImageRecyclerView.setDrawingCacheEnabled(true);
        mCrimeImageRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        updateImageUI();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private void updateImageUI() {
        mAdapter = new CrimeImageListAdapter(this, mPhotoFileList);
        mCrimeImageRecyclerView.setAdapter(mAdapter);
    }

    private class CrimeImageListHolder extends RecyclerView.ViewHolder {

        public ImageView crimeImageView;
        private Activity mActivity;

        public CrimeImageListHolder(Activity activity, View itemView) {
            super(itemView);
            mActivity = activity;
            crimeImageView = itemView.findViewById(R.id.list_item_crime_Image_crimeImageView);
        }

        public void bindCrimeImage(File image) {
            mPhotoFile = image;
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), mActivity);
            crimeImageView.setImageBitmap(bitmap);
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
        public CrimeImageListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
            View view = layoutInflater
                    .inflate(R.layout.list_item_crime_image, parent, false);
            return new CrimeImageListHolder(mActivity, view);
        }

        @Override
        public void onBindViewHolder(CrimeImageListHolder holder, int position) {
            String photoFilename = mCrimePics.get(position);
            mPhotoFile = CrimeLab.get(mActivity).getPhotoFile(mCrime, photoFilename);
            holder.bindCrimeImage(mPhotoFile);
        }

        @Override
        public int getItemCount() {
            return mCrimePics.size();
        }
    }
}
