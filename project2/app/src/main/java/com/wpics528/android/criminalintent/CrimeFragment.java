package com.wpics528.android.criminalintent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class CrimeFragment extends Fragment {

    private static final String TAG = "CrimeFragment";
    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;
    @BindView(R.id.crime_title)
    EditText mTitleField;

    @BindView(R.id.crime_date_button)
    ImageButton mDateButton;
    @BindView(R.id.crime_date)
    TextView mDateEditText;

    @BindView(R.id.crime_solved)
    ImageButton mSolvedButton;
    @BindView(R.id.crime_solved_text)
    TextView mSolvedEditText;

    @BindView(R.id.crime_suspect)
    ImageButton mSuspectButton;
    @BindView(R.id.crime_suspect_text)
    TextView mSuspectEditText;

    @BindView(R.id.crime_report)
    ImageButton mReportButton;

    @BindView(R.id.view_gallery_button)
    ImageButton mViewGalleryButton;
    @BindView(R.id.view_gallery_text)
    TextView mViewGalleryText;

    @BindView(R.id.enable_face_detection_button)
    ImageButton mEnableFaceDetectionButton;
    @BindView(R.id.enable_face_detection_text)
    TextView mEnableFaceDetectionEditText;

    @BindView(R.id.crime_camera)
    ImageButton mPhotoButton;
    @BindView(R.id.crime_photo)
    ImageView mPhotoView;

    private Crime mCrime;
    private File mPhotoFile;
    private Unbinder unbinder;


    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);
        unbinder = ButterKnife.bind(this, v);

        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        updateSolved();
        mSolvedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCrime.setSolved(!mCrime.isSolved());
                updateSolved();
            }
        });

        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectEditText.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        final Intent captureImageCheck = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        boolean canTakePhoto = captureImageCheck.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            mPhotoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File photoFile = null;

                    try {
                        photoFile = CrimeLab.get(getActivity()).createImageFile(mCrime);
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to obtain photo URI", e);
                    }

                    Uri uri = FileProvider.getUriForFile(getActivity(),
                            BuildConfig.APPLICATION_ID + ".provider", photoFile);

                    if (uri != null) {
                        Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                        startActivityForResult(captureImage, REQUEST_PHOTO);
                    } else {
                        Toast.makeText(getActivity(), R.string.error_storage, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        updatePhotoView();

        updateGallery();
        mViewGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(), CrimeImageGalleryActivity.class);
                intent.putExtra(CrimePagerActivity.EXTRA_CRIME_ID, mCrime.getId());

                startActivity(intent);

//                if (mCrime.getPhotoCount() < 16) {
//
//                } else {
//
//                }
            }
        });


        updateFaceDetection();
        mEnableFaceDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCrime.setFaceDetectionEnabled(!mCrime.isFaceDetectionEnabled());
                updateFaceDetection();
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[]{
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectEditText.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            mCrime.setPhotoCount(mCrime.getPhotoCount() + 1);

            if (mPhotoFile == null) {
                mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
                updatePhotoView();
            }

            updateGallery();
        }
    }

    private void updateDate() {
        mDateEditText.setText(DateUtils.getRelativeTimeSpanString(
                mCrime.getDate().getTime(),
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS));
    }

    private void updateGallery() {
        int photoCount = mCrime.getPhotoCount();
        if (photoCount == 0) {
            mViewGalleryText.setVisibility(View.GONE);
        } else if (photoCount == 1) {
            mViewGalleryText.setVisibility(View.VISIBLE);
            mViewGalleryText.setText(getString(R.string.gallery_count_one));
        } else {
            mViewGalleryText.setVisibility(View.VISIBLE);
            mViewGalleryText.setText(getString(R.string.gallery_count, photoCount));
        }
    }

    private void updateSolved() {
        if (mCrime.isSolved()) {
            mSolvedButton.setImageResource(R.drawable.ic_thumb_up);
            mSolvedEditText.setText(R.string.case_solved);
        } else {
            mSolvedButton.setImageResource(R.drawable.ic_thumb_down);
            mSolvedEditText.setText(R.string.case_unsolved);
        }
    }

    private void updateFaceDetection() {
        if (mCrime.isFaceDetectionEnabled()) {
            mEnableFaceDetectionButton.setImageResource(R.drawable.ic_tag_faces);
            mEnableFaceDetectionEditText.setText(R.string.face_detection_enabled);
        } else {
            mEnableFaceDetectionButton.setImageResource(R.drawable.ic_tag_faces_off);
            mEnableFaceDetectionEditText.setText(R.string.face_detection_disabled);
        }
    }

    private String getCrimeReport() {
        String solvedString;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Picasso
                    .with(getActivity())
                    .load(mPhotoFile)
                    .fit()
                    .centerCrop()
                    .into(mPhotoView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
