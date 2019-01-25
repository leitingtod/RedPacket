package com.example.redpacket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (view != null) {
            TextView textViewOpen = view.findViewById(R.id.text_open_delay_time);
            TextView textViewClose = view.findViewById(R.id.text_loading_time);
            final EditText editTextOpen = view.findViewById(R.id.edit_open_delay_time);
            final EditText editTextClose = view.findViewById(R.id.edit_loading_time);

            String so = getResources().getString(R.string.text_open_delay_time) + "(" +
                    getResources().getString(R.string.delay_time_unit) + ")";

            String sc = getResources().getString(R.string.text_loading_time) + "(" +
                    getResources().getString(R.string.delay_time_unit) + ")";

            textViewOpen.setText(so);
            textViewClose.setText(sc);
            editTextOpen.setSelection(editTextOpen.getText().length());
            editTextClose.setSelection(editTextClose.getText().length());

            editTextOpen.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().isEmpty()) return;
                    MonitorService.openDelayTime = Integer.parseInt(s.toString());
                }
            });


            editTextClose.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().isEmpty()) return;
                    MonitorService.loadingTime = Integer.parseInt(s.toString());
                }
            });

            final Button button = view.findViewById(R.id.button_config);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String s = getResources().getString(R.string.text_button_config) + " " +
                                    getResources().getString(R.string.app_name);
                            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
                        }
                    }, 1000);

                    MonitorService.openDelayTime = Integer.parseInt(editTextOpen.getText().toString());
                    MonitorService.loadingTime = Integer.parseInt(editTextClose.getText().toString());

                    Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accessibleIntent);
                }
            });
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
