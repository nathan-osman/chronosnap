package com.nathanosman.chronosnap.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.nathanosman.chronosnap.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Display a dialog prompting the user for information to start a new sequence
 *
 * Currently, this information includes just the sequence name. This may be
 * expanded later to include other information.
 */
public class NewSequenceDialogFragment extends DialogFragment {

    /**
     * Callback interface for dialog events
     */
    public interface NewSequenceDialogListener {

        /**
         * Called when the dialog is accepted and a sequence name chosen
         * @param sequenceName name chosen by the user for the sequence
         */
        void onNewSequenceDialogAccept(CharSequence sequenceName);
    }

    // Callback for dialog events
    private NewSequenceDialogListener mListener;

    /**
     * Register the activity as the listener
     * @param activity parent activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (NewSequenceDialogListener) activity;
    }

    /**
     * Create the dialog from the XML layout
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Create the dialog builder and retrieve the layout inflater
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_new, null);

        // Initialize the edit control with the current date/time
        final String sequenceName = SimpleDateFormat.getDateTimeInstance().format(new Date());
        final EditText editText = (EditText) view.findViewById(R.id.edit_sequenceName);
        editText.setText(sequenceName);
        editText.selectAll();

        // TODO: localize button captions

        // Build the dialog
        builder.setView(view)
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {

                    /**
                     * Start the capture with the specified sequence name
                     */
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onNewSequenceDialogAccept(editText.getText());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    /**
                     * Cancel the dialog and close it
                     */
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NewSequenceDialogFragment.this.getDialog().cancel();
                    }
                });

        // Ensure the keyboard is visible when the dialog opens
        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }
}
