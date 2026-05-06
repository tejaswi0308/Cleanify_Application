package com.example.cleanify_application.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cleanify_application.R;
import com.example.cleanify_application.models.CleaningRequest;

import java.util.List;

public class CompletedTaskAdapter extends RecyclerView.Adapter<CompletedTaskAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CleaningRequest task);
    }

    private List<CleaningRequest> tasks;
    private OnItemClickListener listener;

    public CompletedTaskAdapter(List<CleaningRequest> tasks, OnItemClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CleaningRequest task = tasks.get(position);
        holder.tvRoomNumber.setText("Room " + task.getRoomNumber());
        holder.tvDateTime.setText(task.getDate() + " • " + task.getTimeSlot());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvDateTime, tvVerifiedBadge;

        ViewHolder(View view) {
            super(view);
            tvRoomNumber = view.findViewById(R.id.tvRoomNumber);
            tvDateTime = view.findViewById(R.id.tvDateTime);
            tvVerifiedBadge = view.findViewById(R.id.tvVerifiedBadge);
        }
    }
}
