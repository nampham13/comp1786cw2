package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final Context context;
    private final List<Course> courses;
    private final CourseClickListener listener;

    public CourseAdapter(Context context, List<Course> courses, CourseClickListener listener) {
        this.context = context;
        this.courses = courses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public class CourseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView nameTextView;
        private final TextView typeTextView;
        private final TextView dayTimeTextView;
        private final TextView priceTextView;
        private final TextView capacityTextView;
        private Course course;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.course_name);
            typeTextView = itemView.findViewById(R.id.course_type);
            dayTimeTextView = itemView.findViewById(R.id.course_day_time);
            priceTextView = itemView.findViewById(R.id.course_price);
            capacityTextView = itemView.findViewById(R.id.course_capacity);
            itemView.setOnClickListener(this);
        }

        public void bind(Course course) {
            this.course = course;
            nameTextView.setText(course.getName());
            typeTextView.setText(course.getType());
            dayTimeTextView.setText(String.format("%s at %s", course.getDayOfWeek(), course.getTime()));
            priceTextView.setText(String.format("$%.2f", course.getPrice()));
            capacityTextView.setText(String.format("Capacity: %d", course.getCapacity()));
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        }
    }

    public interface CourseClickListener {
        void onCourseClick(Course course);
    }
}