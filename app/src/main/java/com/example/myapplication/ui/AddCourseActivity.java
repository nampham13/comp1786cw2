package com.example.myapplication.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityAddCourseBinding;
import com.example.myapplication.firebase.FirebaseService;
import com.example.myapplication.model.Course;

public class AddCourseActivity extends AppCompatActivity {
    private ActivityAddCourseBinding binding;
    private FirebaseService firebaseService;
    private boolean isEditMode = false;
    private Course existingCourse;
    
    // Time picker variables
    private Calendar timeCalendar = Calendar.getInstance();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    
    // Loading timeout variables
    private static final int LOADING_TIMEOUT = 15000; // 15 seconds max loading time
    private Handler loadingTimeoutHandler = new Handler();
    private Runnable loadingTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize loading timeout mechanism
        initLoadingTimeout();
        
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        firebaseService = FirebaseService.getInstance();
        
        // Setup day of week spinner with disabled past days
        setupDayOfWeekSpinner();
        
        // Setup course type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.yoga_course_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.typeSpinner.setAdapter(typeAdapter);
        
        // Setup time picker
        setupTimePicker();
        
        // Check if we're in edit mode
        if (getIntent().hasExtra("course_id")) {
            isEditMode = true;
            String courseId = getIntent().getStringExtra("course_id");
            getSupportActionBar().setTitle("Edit Course");
            binding.submitButton.setText("Update Course");
            
            // Load course data
            loadCourseData(courseId);
        } else {
            getSupportActionBar().setTitle("Add New Course");
        }
        
        // Setup submit button
        binding.submitButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (isEditMode) {
                    updateCourse();
                } else {
                    saveCourse();
                }
            }
        });
        
        // Setup preview button
        binding.previewButton.setOnClickListener(v -> {
            if (validateInputs()) {
                showPreview();
            }
        });
    }

    private void loadCourseData(String courseId) {
        showLoading("Loading course data...", "Please wait");
        
        // Set a timeout to show a more informative message if loading takes too long
        Handler messageUpdateHandler = new Handler();
        Runnable messageUpdateRunnable = () -> {
            if (binding.progressOverlay.getVisibility() == View.VISIBLE) {
                binding.loadingSubtext.setText("Still loading... Check your network connection");
            }
        };
        messageUpdateHandler.postDelayed(messageUpdateRunnable, 5000); // 5 seconds timeout
        
        // Create a LiveData observer that we can remove later
        final Observer<Course> courseObserver = new Observer<Course>() {
            @Override
            public void onChanged(Course course) {
                // Hide loading and remove callbacks
                hideLoading();
                messageUpdateHandler.removeCallbacks(messageUpdateRunnable);
                
                // Remove this observer to prevent memory leaks
                firebaseService.getCourseById(courseId).removeObserver(this);
                
                if (course != null) {
                    existingCourse = course;
                    
                    // Populate fields
                    binding.nameEditText.setText(course.getName());
                    binding.descriptionEditText.setText(course.getDescription());
                
                    // Set course type spinner
                    String courseType = course.getType();
                    String[] typesArray = getResources().getStringArray(R.array.yoga_course_types);
                    for (int i = 0; i < typesArray.length; i++) {
                        if (typesArray[i].equals(courseType)) {
                            binding.typeSpinner.setSelection(i);
                            break;
                        }
                    }
                    
                    // In edit mode, we need to allow selection of the original day of week
                    // even if it's in the past, so we need to recreate the spinner adapter
                    String dayOfWeek = course.getDayOfWeek();
                    String[] daysArray = getResources().getStringArray(R.array.days_of_week);
                    
                    // Create a standard adapter without day restrictions for edit mode
                    ArrayAdapter<CharSequence> editDayAdapter = new ArrayAdapter<>(
                            AddCourseActivity.this, android.R.layout.simple_spinner_item, daysArray);
                    editDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.daySpinner.setAdapter(editDayAdapter);
                    
                    // Set the day selection
                    for (int i = 0; i < daysArray.length; i++) {
                        if (daysArray[i].equals(dayOfWeek)) {
                            binding.daySpinner.setSelection(i);
                            break;
                        }
                    }
                    
                    // Set time from course
                    try {
                        // Try to parse the time string to set the time picker
                        String timeStr = course.getTime();
                        SimpleDateFormat parser = new SimpleDateFormat("h:mm a", Locale.getDefault());
                        java.util.Date time = parser.parse(timeStr);
                        if (time != null) {
                            timeCalendar.setTime(time);
                            updateTimeDisplay();
                        }
                    } catch (Exception e) {
                        // If parsing fails, just display the original string
                        binding.timeTextView.setText(course.getTime());
                    }
                    
                    binding.capacityEditText.setText(String.valueOf(course.getCapacity()));
                    binding.durationEditText.setText(String.valueOf(course.getDuration()));
                    binding.priceEditText.setText(String.valueOf(course.getPrice()));
                    
                    // Additional fields can be added here
                } else {
                    Toast.makeText(AddCourseActivity.this, "Failed to load course data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        };
        
        // Observe the LiveData with our custom observer
        firebaseService.getCourseById(courseId).observe(this, courseObserver);
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate name
        if (TextUtils.isEmpty(binding.nameEditText.getText())) {
            binding.nameEditText.setError("Name is required");
            isValid = false;
        }
        
        // Validate capacity
        if (TextUtils.isEmpty(binding.capacityEditText.getText())) {
            binding.capacityEditText.setError("Capacity is required");
            isValid = false;
        } else {
            try {
                int capacity = Integer.parseInt(binding.capacityEditText.getText().toString());
                if (capacity <= 0) {
                    binding.capacityEditText.setError("Capacity must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                binding.capacityEditText.setError("Invalid capacity");
                isValid = false;
            }
        }
        
        // Validate duration
        if (TextUtils.isEmpty(binding.durationEditText.getText())) {
            binding.durationEditText.setError("Duration is required");
            isValid = false;
        } else {
            try {
                int duration = Integer.parseInt(binding.durationEditText.getText().toString());
                if (duration <= 0) {
                    binding.durationEditText.setError("Duration must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                binding.durationEditText.setError("Invalid duration");
                isValid = false;
            }
        }
        
        // Validate price
        if (TextUtils.isEmpty(binding.priceEditText.getText())) {
            binding.priceEditText.setError("Price is required");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(binding.priceEditText.getText().toString());
                if (price < 0) {
                    binding.priceEditText.setError("Price cannot be negative");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                binding.priceEditText.setError("Invalid price");
                isValid = false;
            }
        }
        
        return isValid;
    }

    private Course createCourseFromInputs() {
        String name = binding.nameEditText.getText().toString().trim();
        String type = binding.typeSpinner.getSelectedItem().toString();
        String description = binding.descriptionEditText.getText().toString().trim();
        String dayOfWeek = binding.daySpinner.getSelectedItem().toString();
        String time = timeFormat.format(timeCalendar.getTime());
        int capacity = Integer.parseInt(binding.capacityEditText.getText().toString().trim());
        int duration = Integer.parseInt(binding.durationEditText.getText().toString().trim());
        double price = Double.parseDouble(binding.priceEditText.getText().toString().trim());
        
        Course course = new Course(name, type, description, dayOfWeek, time, capacity, duration, price);
        
        // Add any additional fields
        String additionalInfo = binding.additionalFieldsEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(additionalInfo)) {
            course.addAdditionalField("additionalInfo", additionalInfo);
        }
        
        return course;
    }

    private void saveCourse() {
        showLoading("Saving course...", "Please wait");
        
        // Set a timeout to show a more informative message if saving takes too long
        Handler messageUpdateHandler = new Handler();
        Runnable messageUpdateRunnable = () -> {
            if (binding.progressOverlay.getVisibility() == View.VISIBLE) {
                binding.loadingSubtext.setText("Still saving... Check your network connection");
            }
        };
        messageUpdateHandler.postDelayed(messageUpdateRunnable, 5000); // 5 seconds timeout
        
        Course course = createCourseFromInputs();
        
        // Create a LiveData observer that we can remove later
        final Observer<Boolean> saveObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                // Hide loading and remove callbacks
                hideLoading();
                messageUpdateHandler.removeCallbacks(messageUpdateRunnable);
                
                // Remove this observer to prevent memory leaks
                firebaseService.addCourse(course).removeObserver(this);
                
                if (success) {
                    Toast.makeText(AddCourseActivity.this, "Course saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddCourseActivity.this, "Failed to save course", Toast.LENGTH_SHORT).show();
                }
            }
        };
        
        // Observe the LiveData with our custom observer
        firebaseService.addCourse(course).observe(this, saveObserver);
    }

    private void updateCourse() {
        showLoading("Updating course...", "Please wait");
        
        // Set a timeout to show a more informative message if updating takes too long
        Handler messageUpdateHandler = new Handler();
        Runnable messageUpdateRunnable = () -> {
            if (binding.progressOverlay.getVisibility() == View.VISIBLE) {
                binding.loadingSubtext.setText("Still updating... Check your network connection");
            }
        };
        messageUpdateHandler.postDelayed(messageUpdateRunnable, 5000); // 5 seconds timeout
        
        Course updatedCourse = createCourseFromInputs();
        updatedCourse.setId(existingCourse.getId());
        updatedCourse.setClassInstanceIds(existingCourse.getClassInstanceIds());
        
        // Create a LiveData observer that we can remove later
        final Observer<Boolean> updateObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                // Hide loading and remove callbacks
                hideLoading();
                messageUpdateHandler.removeCallbacks(messageUpdateRunnable);
                
                // Remove this observer to prevent memory leaks
                firebaseService.updateCourse(updatedCourse).removeObserver(this);
                
                if (success) {
                    Toast.makeText(AddCourseActivity.this, "Course updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddCourseActivity.this, "Failed to update course", Toast.LENGTH_SHORT).show();
                }
            }
        };
        
        // Observe the LiveData with our custom observer
        firebaseService.updateCourse(updatedCourse).observe(this, updateObserver);
    }

    private void showPreview() {
        binding.previewContainer.setVisibility(View.VISIBLE);
        binding.formContainer.setVisibility(View.GONE);
        
        Course course = createCourseFromInputs();
        
        binding.previewName.setText(course.getName());
        binding.previewType.setText(course.getType());
        binding.previewDescription.setText(course.getDescription());
        binding.previewDayTime.setText(String.format("%s at %s", course.getDayOfWeek(), course.getTime()));
        binding.previewCapacity.setText(String.format("Capacity: %d", course.getCapacity()));
        binding.previewDuration.setText(String.format("Duration: %d minutes", course.getDuration()));
        binding.previewPrice.setText(String.format("Price: $%.2f", course.getPrice()));
        
        // Show additional fields if any
        String additionalInfo = binding.additionalFieldsEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(additionalInfo)) {
            binding.previewAdditionalInfo.setText(additionalInfo);
            binding.previewAdditionalInfo.setVisibility(View.VISIBLE);
            binding.previewAdditionalInfoLabel.setVisibility(View.VISIBLE);
        } else {
            binding.previewAdditionalInfo.setVisibility(View.GONE);
            binding.previewAdditionalInfoLabel.setVisibility(View.GONE);
        }
        
        binding.editButton.setOnClickListener(v -> {
            binding.previewContainer.setVisibility(View.GONE);
            binding.formContainer.setVisibility(View.VISIBLE);
        });
        
        binding.confirmButton.setOnClickListener(v -> {
            if (isEditMode) {
                updateCourse();
            } else {
                saveCourse();
            }
        });
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
    public void onBackPressed() {
        if (binding.previewContainer.getVisibility() == View.VISIBLE) {
            binding.previewContainer.setVisibility(View.GONE);
            binding.formContainer.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        // Make sure to remove any pending callbacks to prevent memory leaks
        loadingTimeoutHandler.removeCallbacks(loadingTimeoutRunnable);
        
        // Hide loading if it's still showing
        if (binding != null && binding.progressOverlay.getVisibility() == View.VISIBLE) {
            binding.progressOverlay.setVisibility(View.GONE);
        }
        
        super.onDestroy();
    }
    
    /**
     * Initializes the loading timeout mechanism to ensure the loading indicator
     * is never shown indefinitely
     */
    private void initLoadingTimeout() {
        loadingTimeoutRunnable = () -> {
            if (binding.progressOverlay.getVisibility() == View.VISIBLE) {
                Log.w("AddCourseActivity", "Loading timeout reached - forcing hide of loading indicator");
                hideLoading();
                Toast.makeText(this, "Operation timed out. Please try again.", Toast.LENGTH_LONG).show();
            }
        };
    }
    
    /**
     * Shows the loading indicator with the specified message
     */
    private void showLoading(String message, String subMessage) {
        binding.loadingText.setText(message);
        binding.loadingSubtext.setText(subMessage);
        binding.progressOverlay.setVisibility(View.VISIBLE);
        
        // Cancel any existing timeout
        loadingTimeoutHandler.removeCallbacks(loadingTimeoutRunnable);
        
        // Set a new timeout
        loadingTimeoutHandler.postDelayed(loadingTimeoutRunnable, LOADING_TIMEOUT);
    }
    
    /**
     * Hides the loading indicator and cancels any pending timeouts
     */
    private void hideLoading() {
        binding.progressOverlay.setVisibility(View.GONE);
        loadingTimeoutHandler.removeCallbacks(loadingTimeoutRunnable);
    }
    
    /**
     * Sets up the time picker dialog and button
     */
    private void setupTimePicker() {
        // Set default time to 9:00 AM
        timeCalendar.set(Calendar.HOUR_OF_DAY, 9);
        timeCalendar.set(Calendar.MINUTE, 0);
        
        // Update the initial display
        updateTimeDisplay();
        
        // Set up the time picker button click listener
        binding.timePickerButton.setOnClickListener(v -> {
            showTimePickerDialog();
        });
        
        // Also make the text view clickable to show the time picker
        binding.timeTextView.setOnClickListener(v -> {
            showTimePickerDialog();
        });
    }
    
    /**
     * Shows the time picker dialog
     */
    private void showTimePickerDialog() {
        int hour = timeCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = timeCalendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, selectedMinute) -> {
                    timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    timeCalendar.set(Calendar.MINUTE, selectedMinute);
                    updateTimeDisplay();
                },
                hour,
                minute,
                DateFormat.is24HourFormat(this)
        );
        
        timePickerDialog.show();
    }
    
    /**
     * Updates the time display text view with the formatted time
     */
    private void updateTimeDisplay() {
        binding.timeTextView.setText(timeFormat.format(timeCalendar.getTime()));
    }
    
    /**
     * Sets up the day of week spinner with disabled past days
     */
    private void setupDayOfWeekSpinner() {
        String[] daysArray = getResources().getStringArray(R.array.days_of_week);
        
        // Get current day of week (1 = Sunday, 2 = Monday, ..., 7 = Saturday)
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Create custom adapter that disables past days
        DayOfWeekAdapter adapter = new DayOfWeekAdapter(
                this,
                android.R.layout.simple_spinner_item,
                daysArray,
                currentDayOfWeek
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.daySpinner.setAdapter(adapter);
        
        // Set default selection to current day or next available day
        int defaultSelection = mapCalendarDayToArrayIndex(currentDayOfWeek);
        binding.daySpinner.setSelection(defaultSelection);
        
        // Prevent selection of disabled items
        binding.daySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!adapter.isEnabled(position)) {
                    // If user somehow selects a disabled item, revert to default selection
                    Toast.makeText(AddCourseActivity.this, 
                            "Cannot select past days of the week", Toast.LENGTH_SHORT).show();
                    binding.daySpinner.setSelection(defaultSelection);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * Maps Calendar.DAY_OF_WEEK (1-7, Sunday-Saturday) to our array index (0-6, Monday-Sunday)
     */
    private int mapCalendarDayToArrayIndex(int calendarDay) {
        // Our array is [Monday, Tuesday, ..., Sunday]
        // Calendar.DAY_OF_WEEK is [1=Sunday, 2=Monday, ..., 7=Saturday]
        switch (calendarDay) {
            case Calendar.SUNDAY: return 6;    // Sunday is at index 6
            case Calendar.MONDAY: return 0;    // Monday is at index 0
            case Calendar.TUESDAY: return 1;   // Tuesday is at index 1
            case Calendar.WEDNESDAY: return 2; // Wednesday is at index 2
            case Calendar.THURSDAY: return 3;  // Thursday is at index 3
            case Calendar.FRIDAY: return 4;    // Friday is at index 4
            case Calendar.SATURDAY: return 5;  // Saturday is at index 5
            default: return 0;                 // Default to Monday
        }
    }
    
    /**
     * Maps our array index (0-6, Monday-Sunday) to Calendar.DAY_OF_WEEK (1-7, Sunday-Saturday)
     */
    private int mapArrayIndexToCalendarDay(int arrayIndex) {
        // Our array is [Monday, Tuesday, ..., Sunday]
        // Calendar.DAY_OF_WEEK is [1=Sunday, 2=Monday, ..., 7=Saturday]
        switch (arrayIndex) {
            case 0: return Calendar.MONDAY;
            case 1: return Calendar.TUESDAY;
            case 2: return Calendar.WEDNESDAY;
            case 3: return Calendar.THURSDAY;
            case 4: return Calendar.FRIDAY;
            case 5: return Calendar.SATURDAY;
            case 6: return Calendar.SUNDAY;
            default: return Calendar.MONDAY;
        }
    }
    
    /**
     * Custom adapter for day of week spinner that disables past days
     */
    private class DayOfWeekAdapter extends ArrayAdapter<String> {
        private final int currentDayOfWeek;
        
        public DayOfWeekAdapter(Context context, int resource, String[] objects, int currentDayOfWeek) {
            super(context, resource, objects);
            this.currentDayOfWeek = currentDayOfWeek;
        }
        
        @Override
        public boolean isEnabled(int position) {
            // Convert position (array index) to Calendar.DAY_OF_WEEK format
            int dayOfWeek = mapArrayIndexToCalendarDay(position);
            
            // Enable only current day and future days
            return dayOfWeek >= currentDayOfWeek;
        }
        
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            TextView textView = (TextView) view;
            
            if (isEnabled(position)) {
                // Enabled item
                textView.setTextColor(Color.BLACK);
            } else {
                // Disabled item
                textView.setTextColor(Color.GRAY);
            }
            
            return view;
        }
    }
}