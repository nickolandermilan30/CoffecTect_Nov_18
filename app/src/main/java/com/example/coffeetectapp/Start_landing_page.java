package com.example.coffeetectapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;

public class Start_landing_page extends AppCompatActivity {

    ImageButton Next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_landing_page);

        // Hide the action bar (app bar or title bar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        Next = findViewById(R.id.next);

        Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTermsAndConditionsDialog();
            }
        });
    }

    private void showTermsAndConditionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_terms_and_conditions, null);

        // Dapat mayroon kang id para sa CheckBox sa iyong dialog XML.
        CheckBox acceptCheckBox = dialogView.findViewById(R.id.checkBox);

        builder.setView(dialogView)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (acceptCheckBox.isChecked()) {
                            // Handle acceptance of terms here
                            // Redirect to Homepage or perform any desired action
                            Intent intent = new Intent(Start_landing_page.this, Homepage.class);
                            startActivity(intent);
                        } else {
                            // Show a message to inform the user that the checkbox needs to be checked.
                            Toast.makeText(Start_landing_page.this, "Please accept the terms and conditions.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle cancelation or refusal of terms here
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}