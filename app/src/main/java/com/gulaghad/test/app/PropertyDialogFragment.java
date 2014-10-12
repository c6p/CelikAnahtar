package com.gulaghad.test.app;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
        // Use the Builder class for convenient dialog construction
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
//                Log.i("Property", text);
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
        //stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        state.setAdapter(stateAdapter);

        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public Float getFloat(int id, Float val) {
                        String s = ((TextView) view.findViewById(id)).getText().toString();
//                        Log.i("float", s);
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
                                getFloat(R.id.value_min, null),
                                getFloat(R.id.value_max, null),
                                getFloat(R.id.dim_min, null),
                                getFloat(R.id.dim_max, null),
                                getFloat(R.id.temp_min, null),
                                getFloat(R.id.temp_max, null)
                        );
                        ((SuperListener) getTargetFragment()).onOK(p);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //((SuperListener) getTargetFragment()).onCancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}