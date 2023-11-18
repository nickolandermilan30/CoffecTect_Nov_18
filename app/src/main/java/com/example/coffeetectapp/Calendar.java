    package com.example.coffeetectapp;

    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.view.View;
    import android.widget.ArrayAdapter;
    import android.widget.ImageButton;
    import android.widget.LinearLayout;
    import android.widget.LinearLayout.LayoutParams;
    import android.widget.ListView;
    import android.widget.TextView;

    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;

    import com.github.mikephil.charting.charts.PieChart;
    import com.github.mikephil.charting.data.PieData;
    import com.github.mikephil.charting.data.PieDataSet;
    import com.github.mikephil.charting.data.PieEntry;
    import com.github.mikephil.charting.formatter.PercentFormatter;
    import com.github.mikephil.charting.utils.ColorTemplate;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.Map;
    import java.util.Set;

    public class Calendar extends AppCompatActivity {

        private ArrayList<String> diseaseList;
        private ArrayAdapter<String> adapter;

        // SharedPreferences key for storing the disease list
        private static final String PREFS_NAME = "MyPrefs";
        private static final String DISEASE_LIST_KEY = "diseaseList";
        private static final int MAX_SICKNESS_COUNT = 6; // Maximum number of sicknesses
        private static final int TOTAL_PERCENTAGE = 100; // Total percentage for the Pie Chart
        private long diseaseCounter = 1; // Initialize the counter
        ImageButton home, leaf, cam, history, cal;
        View clearButton;
        PieChart pieChart;  // Add this line
        ImageButton legendButton;  // Add this line

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_calendar);

            // Hide the action bar (app bar or title bar)
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            // Initialize the disease list and adapter
            diseaseList = loadDiseaseList();
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, diseaseList);
            pieChart = findViewById(R.id.graphBarChart);
            legendButton = findViewById(R.id.legendButton);

            home = findViewById(R.id.home);
            leaf = findViewById(R.id.leaf);
            cam = findViewById(R.id.cam);
            history = findViewById(R.id.history);
            cal = findViewById(R.id.cal);
            clearButton = findViewById(R.id.clearButton);

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

            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearDiseaseList();
                }
            });

            // Set onClickListener for the legend button
            legendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLegendDialog();
                }
            });

            // Set an onClickListener for the button to generate the pie chart
            findViewById(R.id.generatePieChartButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    generatePieChart();
                }
            });

            // Set the adapter to the ListView
            ListView listView = findViewById(R.id.diseaseListView);
            listView.setAdapter(adapter);

            // Example: Receive the added disease from RecommendationActivity
            Intent intent = getIntent();
            if (intent != null) {
                String addedDisease = intent.getStringExtra("diseaseName");
                if (addedDisease != null) {
                    addDiseaseToList(addedDisease);

                    // Save the updated disease list
                    saveDiseaseList();
                }
            }
        }

        // Method to show the legend dialog
        private void showLegendDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Legend");

            // Inflate the custom layout for the legend dialog
            View view = getLayoutInflater().inflate(R.layout.legend, null);
            builder.setView(view);

            // DiseaseLegend array with names and colors
            DiseaseLegend[] diseaseLegends = {
                    new DiseaseLegend("Cercospora", android.graphics.Color.parseColor("#FF5733")),
                    new DiseaseLegend("Healthy Leaf", android.graphics.Color.parseColor("#33FF57")),
                    new DiseaseLegend("Leaf Miner", android.graphics.Color.parseColor("#3366FF")),
                    new DiseaseLegend("Leaf Rust", android.graphics.Color.parseColor("#FF33CC")),
                    new DiseaseLegend("Phoma", android.graphics.Color.parseColor("#FFFF33")),
                    new DiseaseLegend("Sooty Mold", android.graphics.Color.parseColor("#8C33FF"))
            };

            // Get the LinearLayout from the legend_dialog layout
            LinearLayout legendLayout = view.findViewById(R.id.legendLayout);
            LinearLayout paletteLayout = view.findViewById(R.id.paletteLayout);

            // Add legend TextViews and palette color views dynamically
            for (DiseaseLegend diseaseLegend : diseaseLegends) {
                TextView legendTextView = new TextView(this);
                legendTextView.setText(diseaseLegend.getName());
                legendTextView.setTextColor(diseaseLegend.getColor());

                // Set layout parameters for the TextView
                LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 8); // Adjust margin as needed
                legendTextView.setLayoutParams(params);

                // Add TextView to legendLayout
                legendLayout.addView(legendTextView);

                // Add color views to paletteLayout
                View colorView = new View(this);
                colorView.setId(View.generateViewId());
                colorView.setLayoutParams(new LayoutParams(24, 24)); // Adjust size as needed
                colorView.setBackgroundColor(diseaseLegend.getColor());
                paletteLayout.addView(colorView);
            }

            // Set a button in the dialog to close it
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        private void generatePieChart() {
            // Create entries for the pie chart
            ArrayList<PieEntry> entries = new ArrayList<>();

            // Create a map to store the count for each disease
            HashMap<String, Integer> diseaseCountMap = new HashMap<>();

            // Count occurrences of each disease
            for (String disease : diseaseList) {
                String baseDisease = getBaseDisease(disease);
                if (diseaseCountMap.containsKey(baseDisease)) {
                    // If the base disease is already in the map, increment its count
                    int count = diseaseCountMap.get(baseDisease);
                    diseaseCountMap.put(baseDisease, count + 1);
                } else {
                    // If the base disease is not in the map, add it with count 1
                    diseaseCountMap.put(baseDisease, 1);
                }
            }

            // Calculate total count of diseases
            int totalDiseases = 0;
            for (int count : diseaseCountMap.values()) {
                totalDiseases += count;
            }

            // Calculate the percentage for each disease based on a constant total (TOTAL_PERCENTAGE)
            for (Map.Entry<String, Integer> entry : diseaseCountMap.entrySet()) {
                float percentage = (entry.getValue() / (float) totalDiseases) * TOTAL_PERCENTAGE;
                entries.add(new PieEntry(percentage, entry.getKey()));
            }

            // Create a data set
            PieDataSet dataSet = new PieDataSet(entries, "");

            // Set colors for the data set
            dataSet.setColors(ColorTemplate.COLORFUL_COLORS);

            // Create a data object from the data set
            PieData data = new PieData(dataSet);

            // Set data to the pie chart
            pieChart.setData(data);

            // Set legend to null to hide it
            pieChart.getLegend().setEnabled(false);

            // Set description to an empty string to hide the description label
            pieChart.getDescription().setEnabled(false);

            // Set percentage format for the values
            data.setValueFormatter(new PercentFormatter(pieChart));

            // Set text size and color for the data set
            dataSet.setValueTextSize(10f); // Adjust the size as needed
            dataSet.setValueTextColor(android.graphics.Color.BLACK); // Set text color to black

            // Refresh the pie chart
            pieChart.invalidate();
        }


        // Helper method to get the base disease name
        private String getBaseDisease(String disease) {
            // Implement your logic to extract the base disease name
            // Customize this logic based on your disease naming conventions.

            if (disease.startsWith("Healthy Leaf")) {
                return "Healthy Leaf";
            } else if (disease.startsWith("Cercospora")) {
                return "Cercospora";
            } else if (disease.startsWith("Leaf Miner")) {
                return "Leaf Miner";
            } else if (disease.startsWith("Leaf Rust")) {
                return "Leaf Rust";
            } else if (disease.startsWith("Phoma")) {
                return "Phoma";
            } else if (disease.startsWith("Sooty Mold")) {
                return "Sooty Mold";
            } else {
                return disease; // Default to the original disease name
            }
        }




        // Implement your logic to get the count for each disease
        private int getCountForDisease(String disease) {
            return 0;
        }

        // Method to add a disease to the list
        private void addDiseaseToList(String disease) {
            // Append a unique identifier to each disease to distinguish them
            String uniqueDisease = disease + "_" + System.currentTimeMillis(); // Use timestamp as a unique identifier

            // Disease is not a duplicate, add it to the list
            diseaseList.add(uniqueDisease);
            adapter.notifyDataSetChanged();

            // Save the updated disease list
            saveDiseaseList();
        }


        // Method to add a disease to the list without showing a dialog and without duplicate check
        private void addDiseaseToListWithoutDialog(String disease) {
            // Disease is not a duplicate, add it to the list
            diseaseList.add(disease);
            adapter.notifyDataSetChanged();

            // Save the updated disease list
            saveDiseaseList();
        }



        // Save disease list to SharedPreferences
        private void saveDiseaseList() {
            Set<String> diseaseSet = new HashSet<>(diseaseList);
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(DISEASE_LIST_KEY, diseaseSet);
            editor.apply();
        }

        // Load disease list from SharedPreferences
        private ArrayList<String> loadDiseaseList() {
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

            // Clear the diseaseList locally
            diseaseList.clear();
            adapter.notifyDataSetChanged();
        }
    }
