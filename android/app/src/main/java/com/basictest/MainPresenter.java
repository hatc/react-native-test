package com.basictest;

import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.basictest.utils.BaseObservable;
import com.basictest.utils.Enabler;
import com.basictest.utils.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * subscribe to MainModel, in notify() checks args: { name:String, value:String, oldValue:String }
 * if "busy".equals(property) - enablers.setEnabled(Boolean.of(value))
 * if "view".equals(property) && "photos".equals(value) set back button visible and hide searchBoxLayout
 */

class MainPresenter implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(MainPresenter.class);

    private final MainActivity activity;
    private final MainModel model;
    private Enabler button, edit;
    // create presenter from MainActivity.onCreate, so all View's are already instantiated
    public MainPresenter(MainActivity activity, MainModel model) {
        this.activity = activity; this.model = model;
        activity.onBackPressed();

        final Button getHot = (Button) activity.findViewById(R.id.getHot);
        getHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { MainPresenter.this.model.getHot(); }
        });

        final EditText searchBox = (EditText) activity.findViewById(R.id.searchBox);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (EditorInfo.IME_ACTION_SEARCH == actionId && v.getText().length() > 0) {
                    MainPresenter.this.model.setTag(v.getText().toString());
                    return true;
                }
                return false;
            }
        });
        searchBox.setVisibility(View.INVISIBLE); // rrrrrrrrrrrrrrrrrrrrrrrr

        edit = Enabler.make(searchBox); button = Enabler.make(getHot);

        /*LinearLayout*/ activity.findViewById(R.id.searchBoxLayout); // for hide
    }

    @Override
    public void notify(BaseObservable observable, BaseObservable.Event event) {
        if (activity.isFinishing()) {
            logger.warn("got notify(event.property: \"{}\") then activity is finishing", event.property());
            return;
        }

        if ("busy".equals(event.property())) {
            final boolean v = !Boolean.parseBoolean(event.value());
            button.setEnabled(v);
            edit.setEnabled(v);
        }

        /* if "view".equals(event.property()):
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(value == "photos"); // actually it's should be displayed only if view == photos
        */
    }
}
