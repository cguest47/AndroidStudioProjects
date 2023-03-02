package com.example.sandbox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityTag";

    private static final String CONTACT_1 = "Jay Gatsby";
    private static final String CONTACT_2 = "Melinda Jones";
    private static final String CONTACT_1_PHONE = "14146147955";

    String weatherImageDescription = "displays a variable image of the weather";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View contactCard1 = findViewById(R.id.incl_cardview_contact_card_main_1);
        TextView contactName1 = contactCard1.findViewById(R.id.tv_contact_card);
        contactName1.setText(CONTACT_1);

        TextView contactName2 = findViewById(R.id.incl_cardview_contact_card_main_2).findViewById(R.id.tv_contact_card);
        contactName2.setText(CONTACT_2);

        contactCard1.findViewById(R.id.btn_contact_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel: " + CONTACT_1_PHONE));
                startActivity(callIntent);
            }
        });
    }
}