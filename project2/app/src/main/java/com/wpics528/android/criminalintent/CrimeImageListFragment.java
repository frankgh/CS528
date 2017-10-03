package com.wpics528.android.criminalintent;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.List;
import java.util.UUID;


public class CrimeImageListFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";

    private File mPhotoFile;
    private Crime mCrime;
    private List<String> mpicNames;
    private  RecyclerView mCrimeImageRecyclerView;
    private CrimeImageListAdapter mAdapter;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mpicNames = CrimeLab.get(getActivity()).getPhotoFileList(mCrime);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        mCrimeImageRecyclerView= view.findViewById(R.id.crimeImage_recycler_view);
        mCrimeImageRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateImageUI();

        return view;
    }

    private void updateImageUI() {

            mAdapter = new CrimeImageListAdapter(mpicNames);
            mCrimeImageRecyclerView.setAdapter(mAdapter);

    }

    private class CrimeImageListHolder extends RecyclerView.ViewHolder {

        public ImageView crimeImageView;

        public CrimeImageListHolder(View itemView) {
            super(itemView);

            crimeImageView= (ImageView)itemView.findViewById(R.id.list_item_crime_Image_crimeImageView);
        }


        public void bindCrimeImage(File image) {
            mPhotoFile=image;
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());
             crimeImageView.setImageBitmap(bitmap);

        }


    }

    private class CrimeImageListAdapter extends RecyclerView.Adapter<CrimeImageListHolder> {

        private List<String> mCrimePics;

        public CrimeImageListAdapter(List<String> crimePics) {
            mCrimePics = crimePics;
        }

        @Override
        public CrimeImageListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater
                    .inflate(R.layout.list_item_crime_image, parent, false);
            return new CrimeImageListHolder(view);
        }

        @Override
        public void onBindViewHolder(CrimeImageListHolder holder, int position) {
            String pic = mCrimePics.get(position);
            mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(pic);
            holder.bindCrimeImage(mPhotoFile);
        }

        @Override
        public int getItemCount() {
            return mCrimePics.size();
        }










    }

}

