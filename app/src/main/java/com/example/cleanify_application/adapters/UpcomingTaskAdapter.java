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

public class UpcomingTaskAdapter extends RecyclerView.Adapter<UpcomingTaskAdapter.ViewHolder> {

    private List<CleaningRequest> tasks;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CleaningRequest task);
    }

    public UpcomingTaskAdapter(List<CleaningRequest> tasks, OnItemClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CleaningRequest task = tasks.get(position);
        holder.tvRoomInfo.setText("Room " + task.getRoomNumber() + ", " + task.getHostel() + " Hostel");
        holder.tvTaskDetails.setText(task.getCleaningType() + "  " + task.getTimeSlot());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomInfo, tvTaskDetails;

        ViewHolder(View view) {
            super(view);
            tvRoomInfo = view.findViewById(R.id.tvRoomInfo);
            tvTaskDetails = view.findViewById(R.id.tvTaskDetails);
        }
    }
}
