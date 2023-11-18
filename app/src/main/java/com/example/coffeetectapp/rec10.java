package com.example.coffeetectapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class rec10 extends AppCompatActivity {

    // Map to store recommendations based on disease and severity
    private Map<String, Map<Integer, String>> recommendationsMap = new HashMap<>();
    private String diseaseName; // Declare diseaseName at a higher scope

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec10);

        // Hide the action bar (app bar or title bar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        TextView diseaseTextView = findViewById(R.id.diseaseTextView);
        ImageView mostFrequentImageView = findViewById(R.id.mostFrequentImageView);

        Intent intent = getIntent();
        if (intent != null) {
            diseaseName = SavedResultsManager. getMostFrequentDiseaseHistory10();

            diseaseTextView.setText("Disease: " + diseaseName);

            Bitmap mostFrequentImage = SavedResultsManager. getImageForMostFrequentDiseaseHistory10();
            if (mostFrequentImage != null) {
                mostFrequentImageView.setImageBitmap(mostFrequentImage);
            }
        }

        Button reco10 =findViewById(R.id.leafRecommendationButton);
        Button okButton = findViewById(R.id.okButton);

        reco10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(rec10.this, reco10.class);
                // Pass the most frequent disease name to reco1 activity
                intent.putExtra("diseaseName", diseaseName);
                startActivity(intent);
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(rec10.this, Calendar.class);
                intent.putExtra("diseaseName", diseaseName);
                startActivity(intent);
            }
        });

        TextView mostFrequentTextView = findViewById(R.id.mostFrequentTextView);
        if (diseaseName != null) {
            mostFrequentTextView.setText("Result: " + diseaseName);
        }
    }}
