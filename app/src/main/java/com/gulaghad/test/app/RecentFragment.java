package com.gulaghad.test.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class RecentFragment extends ListFragment {

    private static MainActivity _context = null;
    private static final int FETCH_LENGTH = 32;
    private int _fetchPosition = 0;
    private final List<String> _steels = new ArrayList<String>();
    private final List<Integer> _ids = new ArrayList<Integer>();
    private ArrayAdapter<String> _adapter;

    private String _filter;
    private Pair<Integer, Pair<List<String>, List<Integer>>> _tempResults;
    private boolean _isLoading = false;

    public static RecentFragment newInstance() {
        RecentFragment fragment = new RecentFragment();
        return fragment;
    }

    public RecentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (_context == null)
            _context = (MainActivity) getActivity();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, _steels);
        setListAdapter(_adapter);
        _fetchPosition = 0;
    }

    public void search(String s) {
        if (!s.equals(_filter)) {
            _steels.clear();
            _ids.clear();
            _fetchPosition = 0;
            _filter = s;
        }
        Thread thread =  new Thread(null, fetchMore);
        thread.start();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = getListView();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private final int PADDING = 32;
            private int _firstVisibleItem = 0;
            private int _visibleItemCount = 0;
            private int _totalItemCount = 0;
            private int _scrollState = SCROLL_STATE_IDLE;

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                _firstVisibleItem = firstVisibleItem;
                _visibleItemCount = visibleItemCount;
                _totalItemCount = totalItemCount;
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                _scrollState = scrollState;
                if (_firstVisibleItem + _visibleItemCount + PADDING > _totalItemCount)
                    onScrollCompleted();
            }

            private void onScrollCompleted() {
                Thread thread =  new Thread(null, fetchMore);
                thread.start();
            }
        });
    }

    private final Runnable fetchMore = new Runnable() {
        @Override
        public void run() {
            if (!_isLoading)
            {
                _isLoading = true;
                if (TextUtils.isEmpty(_filter))
                    _tempResults = Pair.create(0, Pair.create((List<String>) new ArrayList<String>(),
                            (List<Integer>) new ArrayList<Integer>()));
                else {
                    _tempResults = _context.getDB().fetchSteel(_fetchPosition, FETCH_LENGTH, _filter);
                }
                _context.runOnUiThread(mergeResults);
            }
        }
    };

    private final Runnable mergeResults = new Runnable() {
        @Override
        public void run() {
            // data fetched
            if (_tempResults.first > _fetchPosition) {
                _fetchPosition = _tempResults.first;
                _steels.addAll(_tempResults.second.first);
                _ids.addAll(_tempResults.second.second);
            }
            _adapter.notifyDataSetChanged();
            _isLoading = false;
        }
    };

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        for (int i=0; i<_ids.size(); i++)
            Log.i("RecentFragment", "id_steel" + i +":"+ _ids.get(i));
        SteelFragment steel = _context.viewSteel(_ids.get(position));
    }

}
