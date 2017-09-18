package com.frankgh.screen3;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.web_view)
    WebView mWebView;
    @BindView(R.id.website1Button)
    Button mWebsiteButton1;
    @BindView(R.id.website2Button)
    Button mWebsiteButton2;
    @BindView(R.id.descriptionTextView)
    TextView mDescriptionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mWebView.getSettings().setJavaScriptEnabled(true);
        loadUrl(mWebsiteButton1);
    }

    @OnClick({R.id.website1Button, R.id.website2Button, R.id.website3Button})
    public void loadUrl(Button view) {

        int url = R.string.website_3_url;
        int description = R.string.website_3_description;

        if (view == mWebsiteButton1) {
            url = R.string.website_1_url;
            description = R.string.website_1_description;
        } else if (view == mWebsiteButton2) {
            url = R.string.website_2_url;
            description = R.string.website_2_description;
        }

        mWebView.loadUrl(getResources().getString(url));
        mDescriptionTextView.setText(description);

        Toast.makeText(this, R.string.good_job, Toast.LENGTH_SHORT).show();
    }
}
