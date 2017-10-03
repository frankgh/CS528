package com.wpics528.android.criminalintent;

import android.support.v4.app.Fragment;


public class CrimeImageListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new CrimeImageListFragment();
    }
}
