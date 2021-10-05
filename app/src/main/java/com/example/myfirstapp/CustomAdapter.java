package com.example.myfirstapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private List<String> data;
    private int selectedPos = RecyclerView.NO_POSITION;
    private View selectedView = null;

    public CustomAdapter (List<String> data){
        this.data = data;
    }

    @NonNull
    @Override
    public CustomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_view, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(CustomAdapter.ViewHolder holder, int position) {
        holder.textView.setText(this.data.get(position));
        holder.itemView.setSelected(selectedPos == position);
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    public boolean isItemSelected() {
        return selectedPos != RecyclerView.NO_POSITION;
    }

    public String getSelectedText() {
        return this.data.get(selectedPos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView textView;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            this.textView = view.findViewById(R.id.textview);
        }

        @Override
        public void onClick(View view) {
            System.out.println("position : " + getLayoutPosition() + " text : " + this.textView.getText());

            if (selectedView != null) {
                selectedView.setBackgroundColor(Color.WHITE);
            }

            selectedPos = getLayoutPosition();
            view.setBackgroundColor(Color.CYAN);
            selectedView = view;
        }
    }
}
