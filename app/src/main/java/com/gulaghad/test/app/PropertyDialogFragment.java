package com.gulaghad.test.app;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PropertyDialogFragment extends DialogFragment {

    View view;

    public interface SuperListener{
        void onOK(SQLiteHelper.SearchProperty property);
    }
    public static PropertyDialogFragment newInstance(SuperListener listener) {
        PropertyDialogFragment fragment = new PropertyDialogFragment();
        fragment.setTargetFragment((Fragment) listener, /*requestCode*/ 1234);
        return fragment;
    }

    public PropertyDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_property, null);

        Spinner property = (Spinner) view.findViewById(R.id.property);
        final LinkedHashMap<String, Pair<Integer, String>> properties =
                SQLiteHelper.get(getActivity().getApplicationContext()).properties();
        final ArrayList<String> propNames = new ArrayList<String>(properties.keySet());
        final TextView minUnit = (TextView) view.findViewById(R.id.valuemin_unit);
        final TextView maxUnit = (TextView) view.findViewById(R.id.valuemax_unit);
        property.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String text = properties.get(propNames.get(position)).second;
                minUnit.setText(text);
                maxUnit.setText(text);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<String> propAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, propNames);
        property.setAdapter(propAdapter);

        Spinner state = (Spinner) view.findViewById(R.id.state);
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.states, android.R.layout.simple_spinner_item);
        state.setAdapter(stateAdapter);
        // set values
        Bundle args = getArguments();
        if (args != null) {
            property.setSelection(propAdapter.getPosition(args.getString("PROPERTY")));
            state.setSelection(stateAdapter.getPosition(args.getString("STATE")));
            ((EditText) view.findViewById(R.id.value_min)).setText(args.getString("VALUE_MIN"));
            ((EditText) view.findViewById(R.id.value_max)).setText(args.getString("VALUE_MAX"));
            ((EditText) view.findViewById(R.id.dim_min)).setText(args.getString("DIM_MIN"));
            ((EditText) view.findViewById(R.id.dim_max)).setText(args.getString("DIM_MAX"));
            ((EditText) view.findViewById(R.id.temp_min)).setText(args.getString("TEMP_MIN"));
            ((EditText) view.findViewById(R.id.temp_max)).setText(args.getString("TEMP_MAX"));
        }

        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public Float getFloat(int id, Float val) {
                        String s = ((TextView) view.findViewById(id)).getText().toString();
                        if (s.equals(""))
                            return val;
                            else
                        return Float.parseFloat(s);
                    }
                    public void onClick(DialogInterface dialog, int id) {
                        Spinner property = ((Spinner) view.findViewById(R.id.property));
                        Spinner state = ((Spinner) view.findViewById(R.id.state));
                        Float t;
                        SQLiteHelper.SearchProperty p = new SQLiteHelper.SearchProperty(
                                property.getSelectedItem().toString(),
                                state.getSelectedItem().toString(),
                                getFloat(R.id.value_min, -1f),
                                getFloat(R.id.value_max, -1f),
                                getFloat(R.id.dim_min, -1f),
                                getFloat(R.id.dim_max, -1f),
                                getFloat(R.id.temp_min, -1f),
                                getFloat(R.id.temp_max, -1f)
                        );
                        ((SuperListener) getTargetFragment()).onOK(p);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }
}