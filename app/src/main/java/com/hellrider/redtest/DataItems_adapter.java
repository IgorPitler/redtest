package com.hellrider.redtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DataItems_adapter extends RecyclerView.Adapter<DataItems_adapter.ViewHolder> {

    private final LayoutInflater inflater;
    public ArrayList<DataItem> items_arr;

    DataItems_adapter(Context context, ArrayList<DataItem> items_arr)
    {
        this.items_arr=items_arr;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public DataItems_adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.data_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataItems_adapter.ViewHolder holder, int position) {
        holder.textViewTitle.setText(items_arr.get(position).getTitle());
        holder.textViewBody.setText(items_arr.get(position).getBody());
    }

    @Override
    public int getItemCount() {
        return this.items_arr.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewTitle;
        final TextView textViewBody;

        ViewHolder(View view){
            super(view);
            textViewTitle = (TextView) view.findViewById(R.id.textViewTitle);
            textViewBody = (TextView) view.findViewById(R.id.textViewBody);
        }
    }
}
