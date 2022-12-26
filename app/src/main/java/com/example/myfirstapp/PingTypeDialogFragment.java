package com.example.myfirstapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.DialogFragment;

/**
 * A fragment for the Ping Type dialog.
 * Use the {@link PingTypeDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PingTypeDialogFragment extends DialogFragment {

    public PingTypeDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PingTypeDialogFragment.
     */
    public static PingTypeDialogFragment newInstance() {
        return new PingTypeDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void onButtonRecurringClick(View view) {
        Log.i(getLogName(), "Recurring clicked.");
        PingTypeDialogFragment.this.getDialog().cancel();
    }

    private String getLogName() {
        return "Ping Type Dialog";
    }

    private void onButtonOnceClick(View view) {
        Log.i(getLogName(), "Once clicked.");
        PingTypeDialogFragment.this.getDialog().cancel();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create the builder.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.fragment_ping_type_dialog, null);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PingTypeDialogFragment.this.getDialog().cancel();
                    }
                });

        // Connect signal handlers.
        view.findViewById(R.id.buttonOnce).setOnClickListener(this::onButtonOnceClick);
        view.findViewById(R.id.buttonRecurring).setOnClickListener(this::onButtonRecurringClick);

        // Return view.
        return builder.create();
    }
}