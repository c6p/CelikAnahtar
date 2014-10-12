package com.gulaghad.test.app;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class CompositionDialogFragment extends DialogFragment {

    private SearchFragment.Composition _c = null;
    private String[] elements;
    View view;

    public interface SuperListener{
        void onOK(SearchFragment.Composition c);
    }
    public static CompositionDialogFragment newInstance(SuperListener listener) {
        CompositionDialogFragment fragment = new CompositionDialogFragment();
        fragment.setTargetFragment((Fragment) listener, /*requestCode*/ 1234);
        return fragment;
    }

    public CompositionDialogFragment() {
        Bundle args = getArguments();
        if (args != null) {
            String name = args.getString("NAME");
            if (name != null) {
                _c = new SearchFragment.Composition(name);
                _c.min = args.getFloat("MIN");
                _c.max = args.getFloat("MAX");
                _c.value = args.getFloat("VALUE");
                _c.tolerance = args.getFloat("TOLERANCE");
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        elements = getResources().getStringArray(R.array.elements);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_composition, null);
        Spinner spinner = (Spinner) view.findViewById(R.id.compo_element);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.elements, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public Float getFloat(int id, int val) {
                        String s = ((TextView) view.findViewById(id)).getText().toString();
                        return s.equals("") ? val : Float.parseFloat(s);
                    }
                    public void onClick(DialogInterface dialog, int id) {
                        Spinner spinner = ((Spinner) view.findViewById(R.id.compo_element));
                        SearchFragment.Composition c = new SearchFragment.Composition(spinner.getSelectedItem().toString());
                        c.min = getFloat(R.id.compo_minimum, -1);
                        c.max = getFloat(R.id.compo_maximum, -1);
                        c.value = getFloat(R.id.compo_value, -1);
                        c.tolerance = getFloat(R.id.compo_tolerance, 0);
                        ((SuperListener) getTargetFragment()).onOK(c);
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