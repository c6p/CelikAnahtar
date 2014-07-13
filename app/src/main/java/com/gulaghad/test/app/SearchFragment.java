package com.gulaghad.test.app;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class SearchFragment extends ListFragment {

    private ArrayList<String> _elements;

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        return fragment;
    }

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, _elements));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showComposition(_elements.get(position).toString());
    }

    void showComposition(String element) {
        DialogFragment newFragment = CompositionDialogFragment.newInstance(element);
        newFragment.show(getFragmentManager(), "composition");
    }
}
