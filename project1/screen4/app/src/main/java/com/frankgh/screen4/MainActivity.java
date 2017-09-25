package com.frankgh.screen4;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.phoneNumberEditText)
    EditText mPhoneNumberEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher_2);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        ButterKnife.bind(this);

        mPhoneNumberEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher("US"));
    }
}
