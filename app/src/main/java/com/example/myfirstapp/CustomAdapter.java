package com.example.myfirstapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

// An adapter for the list view to display Ping contacts. This contains a set
// of values and optional display text.
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private List<String> data;
    private List<String> displayValues = null;
    private int selectedPos = RecyclerView.NO_POSITION;
    private View selectedView = null;
    private SelectionNotifier selectionNotifier = null;

    public CustomAdapter (List<String> data){
        this.data = data;
    }

    // Set the display values to use. This must have a value for each entry.
    public void setDisplayValues(List<String> displayValues) {
        this.displayValues = displayValues;
    }

    // Sort the list in alphabetical order.
    // @pre The display values must already be set.
    public void sortEntries() {
        // Create a sorted map.
        SortedMap<String, String> map = new TreeMap<>();

        // Add entries.
        for (int i = 0; i < data.size(); ++i) {
            String displayText = getDisplayText(i);
            String value = data.get(i);
            map.put(displayText, value);
        }

        // Update the lists.
        data.clear();
        data.addAll(map.values());

        displayValues.clear();
        displayValues.addAll(map.keySet());
    }

    @NonNull
    @Override
    public CustomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_view, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(CustomAdapter.ViewHolder holder, int position) {
        holder.textView.setText(getDisplayText(position));
        holder.itemView.setSelected(selectedPos == position);
    }

    // Get the text to display at the given position.
    private String getDisplayText(int position) {
        // Get value text.
        String text = data.get(position);

        // Override with display text if defined.
        if (displayValues != null) {
            String displayValue = displayValues.get(position);
            if (!displayValue.isEmpty()) {
                text = displayValue;
            }
        }

        // Return display value.
        return text;
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    public boolean isItemSelected() {
        return selectedPos != RecyclerView.NO_POSITION;
    }

    public String getSelectedValue() {
        return this.data.get(selectedPos);
    }

    /**
     * Set the callback for when an item is selected.
     * @param notifier The callback.
     */
    public void setSelectionNotifier(SelectionNotifier notifier) {
        selectionNotifier = notifier;
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

            // Call the callback if required.
            if (selectionNotifier != null) {
                selectionNotifier.onItemSelected();
            }
        }
    }
}
