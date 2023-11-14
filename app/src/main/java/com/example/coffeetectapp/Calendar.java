package com.example.coffeetectapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Calendar extends AppCompatActivity {

    private Toast t;
    private ImageButton home, leaf, cam, history, cal;
    private PieChart pieChart;
    private Button clearButton;
    private String diseaseName;
    private PieDataSet dataSet;
    private ListView diseaseListView;
    private ArrayAdapter<String> diseaseAdapter;
    private Map<String, Integer> diseaseCountMap; // Map to store the count for each disease

    // SharedPreferences key for storing the disease list
    private static final String PREFS_NAME = "MyPrefs";
    private static final String DISEASE_LIST_KEY = "diseaseList";
    private static final String DISEASE_COUNT_KEY = "diseaseCount";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Hide the action bar (app bar or title bar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        home = findViewById(R.id.home);
        leaf = findViewById(R.id.leaf);
        cam = findViewById(R.id.cam);
        history = findViewById(R.id.history);
        cal = findViewById(R.id.cal);

        pieChart = findViewById(R.id.graphBarChart);
        clearButton = findViewById(R.id.clearButton);
        diseaseListView = findViewById(R.id.diseaseListView);

        // Initialize ListView and adapter
        List<String> diseaseList = loadDiseaseList();
        diseaseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, diseaseList);
        diseaseListView.setAdapter(diseaseAdapter);

        diseaseCountMap = loadDiseaseCountMap();

        Button sortButton = findViewById(R.id.sortButton);
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortDataAndRefreshPieChart();
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Calendar.this, Homepage.class);
                startActivity(intent);
            }
        });

        leaf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Calendar.this, Leaf.class);
                startActivity(intent);
            }
        });

        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Calendar.this, Camera_page.class);
                startActivity(intent);
            }
        });

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Calendar.this, Folders.class);
                startActivity(intent);
            }
        });

        cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Calendar.this, Calendar.class);
                startActivity(intent);
            }
        });

        // Retrieve data from RecommendationActivity
        Intent intent = getIntent();
        if (intent != null) {
            diseaseName = intent.getStringExtra("diseaseName");

            // Update the PieChart and TextView based on diseaseName
            updateUI(diseaseName);
        }

        // Initialize PieChart
        initializePieChart();

        // Add back button functionality
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearListView();
            }
        });
    }

    // Add a method to sort the data and refresh the PieChart
    private void sortDataAndRefreshPieChart() {
        // Sort the data based on the disease count
        sortDataByCount();

        // Refresh the PieChart
        refreshPieChart();
    }

    // Add a method to sort the data based on the disease count
    private void sortDataByCount() {
        // Sort the disease entries by count in descending order
        List<PieEntry> pieEntries = DiseaseData.getPieEntries();
        pieEntries.sort(new Comparator<PieEntry>() {
            @Override
            public int compare(PieEntry entry1, PieEntry entry2) {
                // Compare in descending order
                return Float.compare(entry2.getValue(), entry1.getValue());
            }
        });
    }



    // Load disease count map from SharedPreferences
    private Map<String, Integer> loadDiseaseCountMap() {
        Map<String, Integer> countMap = new HashMap<>();
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int size = preferences.getInt(DISEASE_COUNT_KEY, 0);
        for (int i = 0; i < size; i++) {
            String disease = preferences.getString(DISEASE_LIST_KEY + i, "");
            int count = preferences.getInt(disease, 0);
            countMap.put(disease, count);
        }
        return countMap;
    }

    // Clear disease count map from SharedPreferences
    private void clearDiseaseCountMap() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(DISEASE_COUNT_KEY);
        for (String disease : diseaseCountMap.keySet()) {
            editor.remove(disease);
        }
        editor.apply();
    }



    // Update UI method
    private void updateUI(String diseaseName) {
        // Update the PieChart and TextView
        updatePieChart(diseaseName);
        updateListView(diseaseName);
    }

    private void updatePieChart(String diseaseName) {
        // Check if the diseaseName is not null and is not already in the list
        if (diseaseName != null && !existsInPieChart(diseaseName)) {
            float percentage = 100f; // You can set the percentage based on your logic
            PieEntry entry = new PieEntry(percentage, diseaseName);
            DiseaseData.addPieEntry(entry);

            // Refresh the PieChart
            refreshPieChart();
        }
    }

    private boolean existsInPieChart(String diseaseName) {
        for (PieEntry entry : DiseaseData.getPieEntries()) {
            if (entry.getLabel() != null && entry.getLabel().equals(diseaseName)) {
                return true;
            }
        }
        return false;
    }

    // Refresh the PieChart
    private void refreshPieChart() {
        if (dataSet != null) {
            // Clear existing entries
            dataSet.clear();
            // Add all entries again
            dataSet.setValues(DiseaseData.getPieEntries());
            // Notify the data set changed
            dataSet.notifyDataSetChanged();
            // Notify the chart that the data has changed
            pieChart.notifyDataSetChanged();
            // Refresh the chart
            pieChart.invalidate();
        }
    }

    // Update ListView method
    // Update ListView method

    private void updateListView(String diseaseName) {
        // Check if the diseaseName is not null and is not already in the list
        if (diseaseName != null && !existsInPieChart(diseaseName)) {
            float percentage = 100f; // You can set the percentage based on your logic
            PieEntry entry = new PieEntry(percentage, diseaseName);
            DiseaseData.addPieEntry(entry);

            // Increment the count for the disease
            incrementDiseaseCount(diseaseName);

            // Refresh the PieChart
            refreshPieChart();

            // Update the ListView with the new diseaseName and count
            diseaseAdapter.add(String.format(Locale.getDefault(), "%s: %d%%", diseaseName, getDiseaseCount(diseaseName)));
            // Notify the adapter that the data set has changed
            diseaseAdapter.notifyDataSetChanged();

            // Save updated disease list and count map
            saveDiseaseList();
            saveDiseaseCountMap();

            // Call the makeToast method
            makeToast("Added " + diseaseName + " to the list");
        }
    }


    // Save disease count map to SharedPreferences
    private void saveDiseaseCountMap() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DISEASE_COUNT_KEY, diseaseCountMap.size());
        for (Map.Entry<String, Integer> entry : diseaseCountMap.entrySet()) {
            editor.putInt(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    // Function to get the count for a specific disease
    private int getDiseaseCount(String disease) {
        return diseaseCountMap.containsKey(disease) ? diseaseCountMap.get(disease) : 0;
    }

    // Function to increment the count for a specific disease
    private void incrementDiseaseCount(String disease) {
        int count = getDiseaseCount(disease);
        diseaseCountMap.put(disease, count + 1);
    }

    private void clearListView() {
        // Clear the ListView
        diseaseAdapter.clear();
        // Notify the adapter that the data set has changed
        diseaseAdapter.notifyDataSetChanged();

        // Clear the saved disease list
        clearDiseaseList();
    }

    private void initializePieChart() {
        List<PieEntry> entries = new ArrayList<>();

        // Use the actual count for the disease
        int count = getDiseaseCount(diseaseName);
        entries.add(new PieEntry(count, diseaseName));

        dataSet = new PieDataSet(entries, "Disease");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(12f);

        pieChart.getLegend().setEnabled(false);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Disease");
        pieChart.animateY(1000);
    }


    // Function to make a Toast given a string
    private void makeToast(String s) {
        if (t != null) t.cancel();
        t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
        t.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    // Save disease list to SharedPreferences
    private void saveDiseaseList() {
        List<String> diseaseList = new ArrayList<>(diseaseAdapter.getCount());
        for (int i = 0; i < diseaseAdapter.getCount(); i++) {
            diseaseList.add(diseaseAdapter.getItem(i));
        }

        Set<String> diseaseSet = new HashSet<>(diseaseList);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(DISEASE_LIST_KEY, diseaseSet);
        editor.apply();
    }

    // Load disease list from SharedPreferences
    private List<String> loadDiseaseList() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> defaultSet = new HashSet<>(); // default empty set
        Set<String> savedSet = preferences.getStringSet(DISEASE_LIST_KEY, defaultSet);

        // Convert the set to a list
        return new ArrayList<>(savedSet);
    }

    // Clear disease list from SharedPreferences
    private void clearDiseaseList() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(DISEASE_LIST_KEY);
        editor.apply();
    }
}