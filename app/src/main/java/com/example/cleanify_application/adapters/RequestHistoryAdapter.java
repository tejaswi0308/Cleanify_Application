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

public class RequestHistoryAdapter extends RecyclerView.Adapter<RequestHistoryAdapter.ViewHolder> {

    private List<CleaningRequest> requests;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CleaningRequest request);
    }

    public RequestHistoryAdapter(List<CleaningRequest> requests, OnItemClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CleaningRequest request = requests.get(position);
        holder.tvDate.setText(request.getDate() + " • " + request.getTimeSlot());
        String staffNamePrefix = holder.itemView.getContext().getString(R.string.staff_assigned_prefix);
        holder.tvStaffName.setText(staffNamePrefix +
                (request.getAssignedStaffName() != null ? request.getAssignedStaffName() : "Pending"));
        holder.tvCleaningType.setText(request.getCleaningType());

        String status = request.getStatus() != null ? request.getStatus() : "";

        // Status indicator color (left stripe)
        int stripeColor;
        switch (status) {
            case "completed":
                stripeColor = holder.itemView.getContext().getResources().getColor(R.color.status_green);
                break;
            case "in_progress":
                stripeColor = holder.itemView.getContext().getResources().getColor(R.color.status_blue);
                break;
            case "cancelled":
                stripeColor = holder.itemView.getContext().getResources().getColor(R.color.status_red);
                break;
            default:
                stripeColor = holder.itemView.getContext().getResources().getColor(R.color.status_orange);
                break;
        }
        holder.statusIndicator.setBackgroundColor(stripeColor);

        // Status badge text + color
        String badgeText;
        int badgeColor;
        switch (status) {
            case "completed":
                badgeText = holder.itemView.getContext().getString(R.string.done_status);
                badgeColor = holder.itemView.getContext().getResources().getColor(R.color.status_green);
                break;
            case "in_progress":
                badgeText = "In Progress";
                badgeColor = holder.itemView.getContext().getResources().getColor(R.color.status_blue);
                break;
            case "cancelled":
                badgeText = "Cancelled";
                badgeColor = holder.itemView.getContext().getResources().getColor(R.color.status_red);
                break;
            case "assigned":
                badgeText = "Assigned";
                badgeColor = holder.itemView.getContext().getResources().getColor(R.color.status_orange);
                break;
            default:
                badgeText = "Scheduled";
                badgeColor = holder.itemView.getContext().getResources().getColor(R.color.status_orange);
                break;
        }
        if (holder.tvStatusBadge != null) {
            holder.tvStatusBadge.setText(badgeText);
            holder.tvStatusBadge.setTextColor(badgeColor);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(request);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvStaffName, tvCleaningType, tvStatusBadge;
        View statusIndicator;

        ViewHolder(View view) {
            super(view);
            tvDate = view.findViewById(R.id.tvDate);
            tvStaffName = view.findViewById(R.id.tvStaffName);
            tvCleaningType = view.findViewById(R.id.tvCleaningType);
            statusIndicator = view.findViewById(R.id.statusIndicator);
            tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
        }
    }
}
