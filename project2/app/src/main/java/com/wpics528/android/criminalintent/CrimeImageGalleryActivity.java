package com.wpics528.android.criminalintent;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

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
        public CrimeImageListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
            View view = layoutInflater
                    .inflate(R.layout.list_item_crime_image, parent, false);
            return new CrimeImageListHolder(mActivity, view);
        }

        @Override
        public void onBindViewHolder(final CrimeImageListHolder holder, int position) {
            final String currentPhotoPath = "file:" + mStorageFile.getAbsolutePath();
            final String photoFilename = mCrimePics.get(position);

            holder.crimeImageView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        // Wait until layout to call Picasso
                        @Override
                        public void onGlobalLayout() {
                            // Ensure we call this only once
                            holder.crimeImageView.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);

                            Picasso.with(mActivity)
                                    .load(currentPhotoPath + "/" + photoFilename)
                                    .resize(holder.crimeImageView.getWidth(), 0)
                                    .into(holder.crimeImageView);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return mCrimePics.size();
        }
    }
}
