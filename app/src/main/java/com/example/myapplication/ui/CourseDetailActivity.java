package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ClassInstanceAdapter;
import com.example.myapplication.databinding.ActivityCourseDetailBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.ClassInstance;
import com.example.myapplication.model.Course;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseDetailActivity extends AppCompatActivity implements ClassInstanceAdapter.ClassInstanceClickListener {
    private ActivityCourseDetailBinding binding;
    private FirebaseService firebaseService;
    private Course course;
    private ClassInstanceAdapter classInstanceAdapter;
    private List<ClassInstance> classInstances = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseService = FirebaseService.getInstance();

        // Setup RecyclerView
        classInstanceAdapter = new ClassInstanceAdapter(this, classInstances, this);
        binding.classInstancesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.classInstancesRecyclerView.setAdapter(classInstanceAdapter);

        // Get course ID from intent
        String courseId = getIntent().getStringExtra("course_id");
        if (courseId == null) {
            Toast.makeText(this, "Course ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load course data
        loadCourseData(courseId);

        // Setup add class instance button
        binding.addClassInstanceButton.setOnClickListener(v -> showAddClassInstanceDialog());
    }

    private void loadCourseData(String courseId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseService.getCourseById(courseId).observe(this, course -> {
            if (course != null) {
                this.course = course;
                updateUI();
                loadClassInstances();
            } else {
                Toast.makeText(this, "Failed to load course data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        getSupportActionBar().setTitle(course.getName());
        binding.courseType.setText(course.getType());
        binding.courseDescription.setText(course.getDescription());
        binding.courseDayTime.setText(String.format("%s at %s", course.getDayOfWeek(), course.getTime()));
        binding.courseCapacity.setText(String.format("Capacity: %d", course.getCapacity()));
        binding.courseDuration.setText(String.format("Duration: %d minutes", course.getDuration()));
        binding.coursePrice.setText(String.format("Price: $%.2f", course.getPrice()));

        // Show additional fields if any
        if (course.getAdditionalFields() != null && !course.getAdditionalFields().isEmpty()) {
            StringBuilder additionalInfo = new StringBuilder();
            for (String key : course.getAdditionalFields().keySet()) {
                Object value = course.getAdditionalFields().get(key);
                if (value != null) {
                    additionalInfo.append(key).append(": ").append(value.toString()).append("\n");
                }
            }
            if (additionalInfo.length() > 0) {
                binding.courseAdditionalInfo.setText(additionalInfo.toString().trim());
                binding.courseAdditionalInfo.setVisibility(View.VISIBLE);
                binding.courseAdditionalInfoLabel.setVisibility(View.VISIBLE);
            } else {
                binding.courseAdditionalInfo.setVisibility(View.GONE);
                binding.courseAdditionalInfoLabel.setVisibility(View.GONE);
            }
        } else {
            binding.courseAdditionalInfo.setVisibility(View.GONE);
            binding.courseAdditionalInfoLabel.setVisibility(View.GONE);
        }
    }

    private void loadClassInstances() {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseService.getClassInstancesForCourse(course.getId()).observe(this, classInstances -> {
            binding.progressBar.setVisibility(View.GONE);

            if (classInstances != null && !classInstances.isEmpty()) {
                this.classInstances.clear();
                this.classInstances.addAll(classInstances);
                classInstanceAdapter.notifyDataSetChanged();
                binding.emptyView.setVisibility(View.GONE);
                binding.classInstancesRecyclerView.setVisibility(View.VISIBLE);
            } else {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.classInstancesRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void showAddClassInstanceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_class_instance, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Class Instance");
        builder.setView(dialogView);

        // Get references to dialog views
        com.google.android.material.textfield.TextInputEditText dateEditText = dialogView.findViewById(R.id.date_edit_text);
        com.google.android.material.textfield.TextInputEditText teacherEditText = dialogView.findViewById(R.id.teacher_edit_text);
        com.google.android.material.textfield.TextInputEditText commentsEditText = dialogView.findViewById(R.id.comments_edit_text);

        // Setup date picker
        Calendar calendar = Calendar.getInstance();
        dateEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateEditText.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setPositiveButton("Add", null); // Set to null initially
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent automatic dismissal on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String dateString = dateEditText.getText().toString().trim();
            String teacher = teacherEditText.getText().toString().trim();
            String comments = commentsEditText.getText().toString().trim();

            // Validate inputs
            boolean isValid = true;
            if (TextUtils.isEmpty(dateString)) {
                dateEditText.setError("Date is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(teacher)) {
                teacherEditText.setError("Teacher name is required");
                isValid = false;
            }

            if (isValid) {
                try {
                    Date date = dateFormat.parse(dateString);
                    
                    // Verify that the date matches the day of week of the course
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                    String dayName = getDayNameFromCalendar(dayOfWeek);
                    
                    if (!dayName.equalsIgnoreCase(course.getDayOfWeek())) {
                        Toast.makeText(this, "Selected date must be a " + course.getDayOfWeek(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Create and add the class instance
                    ClassInstance classInstance = new ClassInstance(course.getId(), date, teacher, comments);
                    addClassInstance(classInstance);
                    dialog.dismiss();
                } catch (Exception e) {
                    dateEditText.setError("Invalid date format");
                }
            }
        });
    }

    private void addClassInstance(ClassInstance classInstance) {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseService.addClassInstance(classInstance).observe(this, success -> {
            binding.progressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(this, "Class instance added successfully", Toast.LENGTH_SHORT).show();
                loadClassInstances();
            } else {
                Toast.makeText(this, "Failed to add class instance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getDayNameFromCalendar(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            default:
                return "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_edit) {
            Intent intent = new Intent(this, AddCourseActivity.class);
            intent.putExtra("course_id", course.getId());
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course and all its class instances? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse() {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseService.deleteCourse(course.getId()).observe(this, success -> {
            binding.progressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete course", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClassInstanceClick(ClassInstance classInstance) {
        showClassInstanceOptionsDialog(classInstance);
    }

    private void showClassInstanceOptionsDialog(ClassInstance classInstance) {
        String[] options = {"Edit", "Delete", "Cancel"};
        
        new AlertDialog.Builder(this)
                .setTitle("Class Instance Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            showEditClassInstanceDialog(classInstance);
                            break;
                        case 1: // Delete
                            showDeleteClassInstanceConfirmationDialog(classInstance);
                            break;
                        case 2: // Cancel
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void showEditClassInstanceDialog(ClassInstance classInstance) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_class_instance, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Class Instance");
        builder.setView(dialogView);

        // Get references to dialog views
        com.google.android.material.textfield.TextInputEditText dateEditText = dialogView.findViewById(R.id.date_edit_text);
        com.google.android.material.textfield.TextInputEditText teacherEditText = dialogView.findViewById(R.id.teacher_edit_text);
        com.google.android.material.textfield.TextInputEditText commentsEditText = dialogView.findViewById(R.id.comments_edit_text);

        // Populate fields with existing data
        dateEditText.setText(dateFormat.format(classInstance.getDate()));
        teacherEditText.setText(classInstance.getTeacherName());
        commentsEditText.setText(classInstance.getComments());

        // Setup date picker
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(classInstance.getDate());
        dateEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateEditText.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setPositiveButton("Update", null); // Set to null initially
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent automatic dismissal on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String dateString = dateEditText.getText().toString().trim();
            String teacher = teacherEditText.getText().toString().trim();
            String comments = commentsEditText.getText().toString().trim();

            // Validate inputs
            boolean isValid = true;
            if (TextUtils.isEmpty(dateString)) {
                dateEditText.setError("Date is required");
                isValid = false;
            }
            if (TextUtils.isEmpty(teacher)) {
                teacherEditText.setError("Teacher name is required");
                isValid = false;
            }

            if (isValid) {
                try {
                    Date date = dateFormat.parse(dateString);
                    
                    // Verify that the date matches the day of week of the course
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                    String dayName = getDayNameFromCalendar(dayOfWeek);
                    
                    if (!dayName.equalsIgnoreCase(course.getDayOfWeek())) {
                        Toast.makeText(this, "Selected date must be a " + course.getDayOfWeek(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Update the class instance
                    classInstance.setDate(date);
                    classInstance.setTeacherName(teacher);
                    classInstance.setComments(comments);
                    updateClassInstance(classInstance);
                    dialog.dismiss();
                } catch (Exception e) {
                    dateEditText.setError("Invalid date format");
                }
            }
        });
    }

    private void updateClassInstance(ClassInstance classInstance) {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseService.updateClassInstance(classInstance).observe(this, success -> {
            binding.progressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(this, "Class instance updated successfully", Toast.LENGTH_SHORT).show();
                loadClassInstances();
            } else {
                Toast.makeText(this, "Failed to update class instance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteClassInstanceConfirmationDialog(ClassInstance classInstance) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class Instance")
                .setMessage("Are you sure you want to delete this class instance? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteClassInstance(classInstance))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClassInstance(ClassInstance classInstance) {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseService.deleteClassInstance(classInstance.getId()).observe(this, success -> {
            binding.progressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(this, "Class instance deleted successfully", Toast.LENGTH_SHORT).show();
                loadClassInstances();
            } else {
                Toast.makeText(this, "Failed to delete class instance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (course != null) {
            loadCourseData(course.getId());
        }
    }
}