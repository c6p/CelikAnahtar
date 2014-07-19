package com.gulaghad.test.app;

import android.app.Activity;
import android.os.AsyncTask;
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
    private boolean _fetchEnd = false;
    private final List<String> _steels = new ArrayList<String>();
    private final List<Integer> _ids = new ArrayList<Integer>();
    private ArrayAdapter<String> _adapter;

    private String _filter;
    private FetchSteelTask _task = null;
    private SQLiteHelper _db=null;
    //private Pair<Integer, Pair<List<String>, List<Integer>>> _tempResults;

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

    private class FetchSteelTask extends AsyncTask<String, Void, Pair<Integer, Pair<List<String>, List<Integer>>> > {
        @Override
        protected Pair<Integer, Pair<List<String>, List<Integer>>> doInBackground(String... params) {
            Pair<Integer, Pair<List<String>, List<Integer>>> results = null;
            String filter = params[0];
            if (_db != null && !TextUtils.isEmpty(filter))
                results = _db.fetchSteel(_fetchPosition, FETCH_LENGTH, filter);
            return results;
        }
        protected void onPostExecute(Pair<Integer, Pair<List<String>, List<Integer>>> results) {
            // data fetched
            if (results != null && results.first > _fetchPosition) {
                _fetchPosition = results.first;
                _steels.addAll(results.second.first);
                _ids.addAll(results.second.second);
                _adapter.notifyDataSetChanged();
            } else {
                _fetchEnd = true;
            }
            _task = null;
        }
    }
    public void fetch() {
        if (!_fetchEnd && _task == null)
            _task = (FetchSteelTask) new FetchSteelTask().execute(_filter);
    }
    public void cancel() {
        if (_task != null) {
            _task.cancel(true);
            _task = null;
        }
        _fetchEnd = false;
    }

    public void search(String s, SQLiteHelper db) {
        cancel();
        if (!s.equals(_filter)) {
            _steels.clear();
            _ids.clear();
            _fetchPosition = 0;
            _filter = s;
            _db = db;
        }
        fetch();
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
                fetch();
            }
        });
    }

    /*private final Runnable fetchMore = new Runnable() {
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
    };*/

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        for (int i=0; i<_ids.size(); i++)
            Log.i("RecentFragment", "id_steel" + i +":"+ _ids.get(i));
        SteelFragment steel = _context.viewSteel(_ids.get(position));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancel();
    }

}
