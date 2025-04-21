package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.adapter.CourseAdapter;
import com.example.myapplication.cloud.CloudService;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.Course;
import com.example.myapplication.sync.DataSyncService;
import com.example.myapplication.ui.AddCourseActivity;
import com.example.myapplication.ui.CourseDetailActivity;
import com.example.myapplication.ui.SearchActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CourseAdapter.CourseClickListener {
    private ActivityMainBinding binding;
    private CourseAdapter courseAdapter;
    private FirebaseService firebaseService;
    private DataSyncService dataSyncService;
    private CloudService cloudService;
    private List<Course> courseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Initialize services
        firebaseService = FirebaseService.getInstance();
        dataSyncService = DataSyncService.getInstance();
        cloudService = CloudService.getInstance();

        // Setup RecyclerView
        courseAdapter = new CourseAdapter(this, courseList, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(courseAdapter);

        // Setup FAB
        binding.fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddCourseActivity.class);
            startActivity(intent);
        });

        // Setup sync button
        binding.syncButton.setOnClickListener(v -> syncData());
        
        // Set default network status
        binding.networkStatusText.setText("Online");
        binding.networkStatusText.setTextColor(getResources().getColor(
                android.R.color.holo_green_dark, getTheme()));

        // Load courses
        loadCourses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_reset) {
            showResetConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadCourses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        firebaseService.getAllCourses().observe(this, courses -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (courses != null && !courses.isEmpty()) {
                courseList.clear();
                courseList.addAll(courses);
                courseAdapter.notifyDataSetChanged();
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
            } else {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void syncData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Use the new CloudService to get available courses
        cloudService.getAvailableCourses().observe(this, courses -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (courses != null && !courses.isEmpty()) {
                courseList.clear();
                courseList.addAll(courses);
                courseAdapter.notifyDataSetChanged();
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                
                // Subscribe to notifications for each course
                for (Course course : courses) {
                    cloudService.subscribeToCourse(course.getId());
                }
                
                Toast.makeText(this, "Data synchronized successfully", Toast.LENGTH_SHORT).show();
            } else {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
                Toast.makeText(this, "No courses available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset All Data")
                .setMessage("Are you sure you want to reset all data? This action cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> resetAllData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetAllData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        firebaseService.resetAllData().observe(this, success -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (success) {
                Toast.makeText(this, "All data has been reset", Toast.LENGTH_SHORT).show();
                loadCourses();
            } else {
                Toast.makeText(this, "Failed to reset data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCourseClick(Course course) {
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra("course_id", course.getId());
        startActivity(intent);
        
        // Subscribe to notifications for this course
        cloudService.subscribeToCourse(course.getId()).observe(this, success -> {
            if (success) {
                Log.d("MainActivity", "Subscribed to course: " + course.getName());
            }
        });
    }
}