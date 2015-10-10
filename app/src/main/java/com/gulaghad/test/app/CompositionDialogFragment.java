package com.gulaghad.test.app;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class CompositionDialogFragment extends DialogFragment {

    //private SearchFragment.Composition _c = null;
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
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        elements = getResources().getStringArray(R.array.elements);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_composition, null);
        Spinner spinner = (Spinner) view.findViewById(R.id.compo_element);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.elements, android.R.layout.simple_spinner_item);
        spinner.setAdapter(adapter);
        // set values
        Bundle args = getArguments();
        if (args != null) {
            spinner.setSelection(adapter.getPosition(args.getString("NAME")));
            ((EditText) view.findViewById(R.id.compo_minimum)).setText(args.getString("MIN"));
            ((EditText) view.findViewById(R.id.compo_maximum)).setText(args.getString("MAX"));
            ((EditText) view.findViewById(R.id.compo_value)).setText(args.getString("VALUE"));
            ((EditText) view.findViewById(R.id.compo_tolerance)).setText(args.getString("TOLERANCE"));
        }

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
                    public void onClick(DialogInterface dialog, int id) { }
                });
        return builder.create();
    }
}