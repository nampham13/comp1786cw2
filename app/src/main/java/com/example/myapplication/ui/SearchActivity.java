package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ClassInstanceAdapter;
import com.example.myapplication.databinding.ActivitySearchBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.ClassInstance;
import com.example.myapplication.model.Course;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity implements ClassInstanceAdapter.ClassInstanceClickListener {
    private ActivitySearchBinding binding;
    private FirebaseService firebaseService;
    private ClassInstanceAdapter classInstanceAdapter;
    private List<ClassInstance> classInstances = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
    private Date selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Search Classes");

        firebaseService = FirebaseService.getInstance();

        // Setup RecyclerView
        classInstanceAdapter = new ClassInstanceAdapter(this, classInstances, this);
        binding.resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.resultsRecyclerView.setAdapter(classInstanceAdapter);

        // Setup search type spinner
        ArrayAdapter<CharSequence> searchTypeAdapter = ArrayAdapter.createFromResource(
                this, R.array.search_types, android.R.layout.simple_spinner_item);
        searchTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.searchTypeSpinner.setAdapter(searchTypeAdapter);

        // Setup day of week spinner
        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(
                this, R.array.days_of_week, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.daySpinner.setAdapter(dayAdapter);

        // Setup search type change listener
        binding.searchTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateSearchInputVisibility(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Setup date picker
        binding.dateEditText.setOnClickListener(v -> showDatePicker());

        // Setup search button
        binding.searchButton.setOnClickListener(v -> performSearch());
    }

    private void updateSearchInputVisibility(int searchTypePosition) {
        // Hide all inputs first
        binding.teacherInputLayout.setVisibility(View.GONE);
        binding.dateInputLayout.setVisibility(View.GONE);
        binding.daySpinner.setVisibility(View.GONE);
        binding.dayLabel.setVisibility(View.GONE);

        // Show the appropriate input based on search type
        String searchType = binding.searchTypeSpinner.getSelectedItem().toString();
        if (searchType.equals("Teacher")) {
            binding.teacherInputLayout.setVisibility(View.VISIBLE);
        } else if (searchType.equals("Date")) {
            binding.dateInputLayout.setVisibility(View.VISIBLE);
        } else if (searchType.equals("Day of Week")) {
            binding.daySpinner.setVisibility(View.VISIBLE);
            binding.dayLabel.setVisibility(View.VISIBLE);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDate = calendar.getTime();
                    binding.dateEditText.setText(dateFormat.format(selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void performSearch() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);
        binding.resultsRecyclerView.setVisibility(View.GONE);

        String searchType = binding.searchTypeSpinner.getSelectedItem().toString();
        
        if (searchType.equals("Teacher")) {
            String teacherName = binding.teacherEditText.getText().toString().trim();
            if (TextUtils.isEmpty(teacherName)) {
                binding.teacherEditText.setError("Please enter a teacher name");
                binding.progressBar.setVisibility(View.GONE);
                return;
            }
            searchByTeacher(teacherName);
        } else if (searchType.equals("Date")) {
            if (selectedDate == null) {
                binding.dateEditText.setError("Please select a date");
                binding.progressBar.setVisibility(View.GONE);
                return;
            }
            searchByDate(selectedDate);
        } else if (searchType.equals("Day of Week")) {
            String dayOfWeek = binding.daySpinner.getSelectedItem().toString();
            searchByDay(dayOfWeek);
        }
    }

    private void searchByTeacher(String teacherName) {
        firebaseService.searchClassInstancesByTeacher(teacherName).observe(this, results -> {
            binding.progressBar.setVisibility(View.GONE);
            updateSearchResults(results);
        });
    }

    private void searchByDate(Date date) {
        firebaseService.searchClassInstancesByDate(date).observe(this, results -> {
            binding.progressBar.setVisibility(View.GONE);
            updateSearchResults(results);
        });
    }

    private void searchByDay(String dayOfWeek) {
        firebaseService.searchCoursesByDay(dayOfWeek).observe(this, courses -> {
            binding.progressBar.setVisibility(View.GONE);
            
            if (courses != null && !courses.isEmpty()) {
                // For each course, get its class instances
                List<String> courseIds = new ArrayList<>();
                for (Course course : courses) {
                    courseIds.add(course.getId());
                }
                
                // We need to track when all queries are complete
                binding.progressBar.setVisibility(View.VISIBLE);
                final int[] completedQueries = {0};
                final List<ClassInstance> allClassInstances = new ArrayList<>();
                
                for (String courseId : courseIds) {
                    firebaseService.getClassInstancesForCourse(courseId).observe(this, classInstances -> {
                        completedQueries[0]++;
                        
                        if (classInstances != null && !classInstances.isEmpty()) {
                            allClassInstances.addAll(classInstances);
                        }
                        
                        // Check if all queries are complete
                        if (completedQueries[0] == courseIds.size()) {
                            binding.progressBar.setVisibility(View.GONE);
                            updateSearchResults(allClassInstances);
                        }
                    });
                }
            } else {
                updateSearchResults(new ArrayList<>());
            }
        });
    }

    private void updateSearchResults(List<ClassInstance> results) {
        classInstances.clear();
        
        if (results != null && !results.isEmpty()) {
            classInstances.addAll(results);
            binding.emptyView.setVisibility(View.GONE);
            binding.resultsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.resultsRecyclerView.setVisibility(View.GONE);
        }
        
        classInstanceAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClassInstanceClick(ClassInstance classInstance) {
        // Get the course for this class instance
        firebaseService.getCourseById(classInstance.getCourseId()).observe(this, course -> {
            if (course != null) {
                Intent intent = new Intent(this, CourseDetailActivity.class);
                intent.putExtra("course_id", course.getId());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Could not find the course for this class", Toast.LENGTH_SHORT).show();
            }
        });
    }
}