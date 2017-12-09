package com.frankgh.wpiparking.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frankgh.wpiparking.R;

/**
 * Simple list-based Activity to redirect to one of the other Activities. This Activity does not
 * contain any useful code related to Firebase Authentication. You may want to start with
 * one of the following Files:
 * {@link GoogleLoginActivity}
 * {@link FacebookLoginActivity}
 * {@link AnonymousAuthActivity}
 */
public class ChooserActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "ChooserActivity";

    private static final Class[] CLASSES = new Class[]{
            GoogleLoginActivity.class,
            FacebookLoginActivity.class,
            AnonymousAuthActivity.class
    };

    private static final int[] DESCRIPTION_IDS = new int[]{
            R.string.desc_google_sign_in,
            R.string.desc_facebook_login,
            R.string.desc_anonymous_auth
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);

        // Set up ListView and Adapter
        ListView listView = findViewById(R.id.list_view);

        AuthMethodsArrayAdapter adapter =
                new AuthMethodsArrayAdapter(this, android.R.layout.simple_list_item_2, CLASSES);
        adapter.setDescriptionIds(DESCRIPTION_IDS);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Class clicked = CLASSES[position];
        startActivity(new Intent(this, clicked));
    }

    public static class AuthMethodsArrayAdapter extends ArrayAdapter<Class> {

        private Context mContext;
        private Class[] mClasses;
        private int[] mDescriptionIds;

        public AuthMethodsArrayAdapter(Context context, int resource, Class[] objects) {
            super(context, resource, objects);

            mContext = context;
            mClasses = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (convertView == null) {
                //ViewHolder viewHolder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.simple_list_item_2, null);

                //viewHolder.button = (Button) convertView.findViewById(R.id.list_item_btn);
                //convertView.setTag(viewHolder);
            }

            ((TextView) view.findViewById(android.R.id.text1)).setText(mClasses[position].getSimpleName());
            ((TextView) view.findViewById(android.R.id.text2)).setText(mDescriptionIds[position]);

            return view;
        }

        public void setDescriptionIds(int[] descriptionIds) {
            mDescriptionIds = descriptionIds;
        }
    }
    public class ViewHolder {

        Button button;
    }
}
