package com.basictest.utils;

import android.text.method.KeyListener;
import android.widget.Button;
import android.widget.EditText;

public abstract class Enabler {
    public static Enabler make(final EditText editText) {
        return new Enabler() {
            private EditText widget = editText;
            private KeyListener keyListener = editText.getKeyListener();
            private boolean enabled = editText.getKeyListener() != null;

            @Override
            public void setEnabled(boolean v) {
                if (enabled != v) {
                    enabled = v;
                    editText.setKeyListener(enabled ? keyListener : null);
                }
            }
        };
    }

    public static Enabler make(final Button button) {
        return new Enabler() {
            private Button widget = button;
            private boolean enabled = button.isClickable();

            @Override
            public void setEnabled(boolean v) {
                if (enabled != v) {
                    enabled = v;
                    widget.setEnabled(v);
                    widget.setClickable(v);
                }
            }
        };
    }

    public abstract void setEnabled(boolean v);
}
