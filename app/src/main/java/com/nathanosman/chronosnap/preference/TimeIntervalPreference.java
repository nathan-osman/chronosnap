package com.nathanosman.chronosnap.preference;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import com.nathanosman.chronosnap.R;


/**
 * Custom preference type for entering a time interval
 * @author sravan953
 */
public class TimeIntervalPreference extends DialogPreference {

    // Handy values for time conversion
    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;

    // References to the pickers
    private NumberPicker mHourPicker;
    private NumberPicker mMinutePicker;
    private NumberPicker mSecondPicker;

    /**
     * Initialize the dialog with the custom layout
     */
    public TimeIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.pref_time);
    }

    /**
     * Perform initialization of the view items
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Retrieve the current value
        long currentValue = Long.valueOf(getPersistedString(
                getContext().getString(R.string.pref_interval_default)));

        // Obtain references to the pickers and initialize them with the correct value
        mHourPicker = (NumberPicker) view.findViewById(R.id.hourPicker);
        mHourPicker.setMinValue(0);
        mHourPicker.setMaxValue(23);
        mHourPicker.setValue((int) (currentValue / HOUR));

        mMinutePicker = (NumberPicker) view.findViewById(R.id.minutePicker);
        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(59);
        mMinutePicker.setValue((int) (currentValue / MINUTE));

        mSecondPicker = (NumberPicker) view.findViewById(R.id.secondPicker);
        mSecondPicker.setMinValue(0);
        mSecondPicker.setMaxValue(59);
        mSecondPicker.setValue((int) (currentValue / SECOND));
    }

    /**
     * Store the value of the pickers
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {

        if (positiveResult) {

            // Calculate the total time in millis
            long currentValue = mHourPicker.getValue() * HOUR +
                    mMinutePicker.getValue() * MINUTE + mSecondPicker.getValue() * SECOND;
            persistString(String.valueOf(currentValue));
        }
    }
}