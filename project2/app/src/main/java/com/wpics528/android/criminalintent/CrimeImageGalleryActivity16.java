package com.wpics528.android.criminalintent;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.GridLayoutManager;
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

public class CrimeImageGalleryActivity16 extends AppCompatActivity {

    @BindView(R.id.crimeImage_recycler_view2)
    RecyclerView mCrimeImageRecyclerView;
    private File mPhotoFile;
    private Crime mCrime;
    private List<String> mPhotoFileList;
    private CrimeImageListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_image_gallery2);
        ButterKnife.bind(this);

        UUID crimeId = (UUID) getIntent().getSerializableExtra(CrimePagerActivity.EXTRA_CRIME_ID);
        mCrime = CrimeLab.get(this).getCrime(crimeId);
        mPhotoFileList = CrimeLab.get(this).getPhotoFileList(mCrime);

        mCrimeImageRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        updateImageUI();
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
            crimeImageView = itemView.findViewById(R.id.list_item_crime_Image_crimeImageView2);
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
                    .inflate(R.layout.recyclerview_item, parent, false);
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
