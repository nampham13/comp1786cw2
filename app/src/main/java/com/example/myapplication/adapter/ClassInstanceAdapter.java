package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ClassInstance;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ClassInstanceAdapter extends RecyclerView.Adapter<ClassInstanceAdapter.ClassInstanceViewHolder> {
    private final Context context;
    private final List<ClassInstance> classInstances;
    private final ClassInstanceClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

    public ClassInstanceAdapter(Context context, List<ClassInstance> classInstances, ClassInstanceClickListener listener) {
        this.context = context;
        this.classInstances = classInstances;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClassInstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_class_instance, parent, false);
        return new ClassInstanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassInstanceViewHolder holder, int position) {
        ClassInstance classInstance = classInstances.get(position);
        holder.bind(classInstance);
    }

    @Override
    public int getItemCount() {
        return classInstances.size();
    }

    public class ClassInstanceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView dateTextView;
        private final TextView teacherTextView;
        private final TextView commentsTextView;
        private final TextView statusTextView;
        private ClassInstance classInstance;

        public ClassInstanceViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.class_date);
            teacherTextView = itemView.findViewById(R.id.class_teacher);
            commentsTextView = itemView.findViewById(R.id.class_comments);
            statusTextView = itemView.findViewById(R.id.class_status);
            itemView.setOnClickListener(this);
        }

        public void bind(ClassInstance classInstance) {
            this.classInstance = classInstance;
            dateTextView.setText(dateFormat.format(classInstance.getDate()));
            teacherTextView.setText("Teacher: " + classInstance.getTeacherName());
            
            if (classInstance.getComments() != null && !classInstance.getComments().isEmpty()) {
                commentsTextView.setText(classInstance.getComments());
                commentsTextView.setVisibility(View.VISIBLE);
            } else {
                commentsTextView.setVisibility(View.GONE);
            }
            
            if (classInstance.isCancelled()) {
                statusTextView.setText("CANCELLED");
                statusTextView.setVisibility(View.VISIBLE);
                statusTextView.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark, context.getTheme()));
            } else {
                statusTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onClassInstanceClick(classInstance);
            }
        }
    }

    public interface ClassInstanceClickListener {
        void onClassInstanceClick(ClassInstance classInstance);
    }
}