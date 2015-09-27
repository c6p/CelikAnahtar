package com.gulaghad.test.app;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class RecentFragment extends ListFragment implements IDataRequester<SQLiteHelper.SteelList> {

    private OnSteelSelectedListener _steelListener;
    private static final int FETCH_LENGTH = 32;
    private int _fetchPosition = 0;
//    private boolean _fetchEnd = false;
    private final List<String> _steels = new ArrayList<String>();
    private final List<Integer> _ids = new ArrayList<Integer>();
    private ArrayAdapter<String> _adapter;
    ListView _listView;
    private boolean _isComplete = false;
//    IDataNegotiator<SQLiteHelper.SteelList> _negotiator;
    private String _dataFilter = new String();

//    private String _filter;
//    private FetchSteelTask _task = null;
//    private SQLiteHelper _db=null;
    //private Pair<Integer, Pair<List<String>, List<Integer>>> _tempResults;
//
    public static RecentFragment newInstance() {
        RecentFragment fragment = new RecentFragment();
//        Bundle args = new Bundle();
//        args.putSerializable("NEGOTIATOR", negotiator);
//        fragment.setArguments(args);
        return fragment;
    }

    public RecentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (_activity == null)
//            _activity = (MainActivity) getActivity();
//        _negotiator = (IDataNegotiator<SQLiteHelper.SteelList>) getArguments().getSerializable("NEGOTIATOR");
//        assert _negotiator != null;
//        _negotiator = Negotiator.get(SQLiteHelper.SteelList.class);
//        _negotiator.register(this);
        Register.steelListDataRequester.set(this);
    }

    @Override
    public void onDestroy() {
//        _negotiator.unregister(this);
        Register.steelListDataRequester.unset(this);
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _steelListener = (OnSteelSelectedListener) activity;
        _steels.clear();
        _ids.clear();
        _adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, _steels);
        setListAdapter(_adapter);
        _fetchPosition = 0;
    }

    @Override
    public void dataReady() {
        Log.i("DATA", "ready");
        if (_listView == null)
            return;
        loadSteels();
    }

    private void requestSteels(int size) {
        if (_isComplete)
            return;
        IDataProvider<SQLiteHelper.SteelList> provider = Register.steelListDataProvider.get();
        if (provider == null) return;
        provider.requestData(size);
    }

    private void loadSteels() {
        IDataProvider<SQLiteHelper.SteelList> provider = Register.steelListDataProvider.get();
        if (provider == null) return;

        SQLiteHelper.SteelList data = provider.getData();
        if (data == null) {
            _isComplete = false;
            _steels.clear();
            _ids.clear();
        } else {
            assert data.steelNames.size() == data.steelIds.size();

            _isComplete = data.complete;
            int start = _steels.size();
            if (!_dataFilter.equals(data.filter)) {
                _steels.clear();
                _ids.clear();
                start = 0;
            }
            int end = data.steelNames.size();
            Log.i(Integer.toString(start), Integer.toString(end));
            _steels.addAll(start, data.steelNames.subList(start, end));
            _ids.addAll(start, data.steelIds.subList(start, end));
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
                    requestSteels(size);
            }

        });
        super.onViewCreated(view, savedInstanceState);
        loadSteels();
    }

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        for (int i=0; i<_ids.size(); i++)
            Log.i("RecentFragment", "id_steel" + i +":"+ _ids.get(i));
        _steelListener.onSteelSelected(_ids.get(position));
        //SteelFragment steel = _context.viewSteel(_ids.get(position));
    }

}
