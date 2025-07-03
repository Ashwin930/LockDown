package com.ashwinsagar.lockdown;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class TimeInputWatcher implements TextWatcher {

    private final EditText editText;
    private final int maxValue;
    private String previousValue = "";

    public TimeInputWatcher(EditText editText, int maxValue) {
        this.editText = editText;
        this.maxValue = maxValue;
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        previousValue = s.toString(); // Save previous valid input
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Not needed here
    }

    @Override
    public void afterTextChanged(Editable s) {
        String input = s.toString();
        if (!input.isEmpty()) {
            try {
                int value = Integer.parseInt(input);
                if (value < 0 || value > maxValue) {
                    int corrected = Math.min(Math.max(0, value), maxValue);
                    editText.setText(String.valueOf(corrected));
                    editText.setSelection(editText.getText().length());
                }
            } catch (NumberFormatException e) {
                editText.setText(previousValue); // Restore valid value if parsing fails
                editText.setSelection(previousValue.length());
            }
        }
    }


}
