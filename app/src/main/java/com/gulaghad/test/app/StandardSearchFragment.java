package com.gulaghad.test.app;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class StandardSearchFragment extends ListFragment implements IDataRequester<SQLiteHelper.StandardList> {

    private OnStandardSelectedListener _standardListener;
    private final List<SQLiteHelper.Standard> _standards = new ArrayList<SQLiteHelper.Standard>();
    private ArrayAdapter<SQLiteHelper.Standard> _adapter;
    ListView _listView;
    private boolean _isComplete = false;
    private String _dataFilter = new String();

    public static StandardSearchFragment newInstance() {
        StandardSearchFragment fragment = new StandardSearchFragment();
        return fragment;
    }

    public StandardSearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Register.standardListDataRequester.set(this);
    }

    @Override
    public void onDestroy() {
        Register.standardListDataRequester.unset(this);
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _standardListener = (OnStandardSelectedListener) activity;
        _standards.clear();
        _adapter = new ArrayAdapter<SQLiteHelper.Standard>(getActivity(),
                android.R.layout.simple_list_item_2, android.R.id.text1, _standards) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(_standards.get(position).name);
                text2.setText(Html.fromHtml(_standards.get(position).title));
                return view;
            }
        };
        setListAdapter(_adapter);
    }

    @Override
    public void dataReady() {
        if (_listView == null)
            return;
        loadStandards();
    }

    private void requestStandards(int size) {
        if (_isComplete)
            return;
        IDataProvider<SQLiteHelper.StandardList> provider = Register.standardListDataProvider.get();
        if (provider == null) return;
        provider.requestData(size);
    }

    private void loadStandards() {
        IDataProvider<SQLiteHelper.StandardList> provider = Register.standardListDataProvider.get();
        if (provider == null) return;

        SQLiteHelper.StandardList data = provider.getData();
        if (data == null) {
            _isComplete = false;
            _standards.clear();
        } else {
            _isComplete = data.complete;
            int start = _standards.size();
            if (!_dataFilter.equals(data.filter)) {
                _standards.clear();
                start = 0;
            }
            int end = data.standards.size();
            Log.i(Integer.toString(start), Integer.toString(end));
            _standards.addAll(start, data.standards.subList(start, end));
        }
        _adapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        _listView = getListView();
        _listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private final int PADDING = 32;
            private int _firstVisibleItem = 0;
            private int _visibleItemCount = 0;
            private int _totalItemCount = 0;

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                _firstVisibleItem = firstVisibleItem;
                _visibleItemCount = visibleItemCount;
                _totalItemCount = totalItemCount;
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                int size = _firstVisibleItem + _visibleItemCount + PADDING;
                if (size > _totalItemCount)
                    requestStandards(size);
            }

        });
        super.onViewCreated(view, savedInstanceState);
        loadStandards();
    }

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        _standardListener.onStandardSelected(_standards.get(position).id);
    }
}
