package com.nathanosman.chronosnap.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.nathanosman.chronosnap.R;

/**
 * @author sravan953
 */
public class TimeIntervalPreference extends DialogPreference {
    Spinner spinner;
    EditText editText;

    public TimeIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.pref_time);
    }

    @Override
    protected void onBindDialogView(View view) {
        spinner = (Spinner)view.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(view.getContext(), R.array.pref_interval_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        editText = (EditText)view.findViewById(R.id.timeInterval);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            String type = spinner.getSelectedItem().toString();
            int time = Integer.parseInt(editText.getText().toString());

            SharedPreferences.Editor pEditor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            pEditor.putString(getContext().getResources().getString(R.string.pref_interval_type_key), type).commit();
            pEditor.putString(getContext().getResources().getString(R.string.pref_interval_key), String.valueOf(time)).commit();
        }
    }
}