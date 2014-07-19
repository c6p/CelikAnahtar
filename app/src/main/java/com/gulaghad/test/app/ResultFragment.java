package com.gulaghad.test.app;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ResultFragment extends Fragment {

    private static MainActivity _context = null;
    private static final int FETCH_LENGTH = 256;
    private int _fetchPosition = 0;
    private boolean _fetchEnd = false;
    //private final List<Spanned> _steels = new ArrayList<Spanned>();
    //private final List<Integer> _ids = new ArrayList<Integer>();
    //private ArrayAdapter<Spanned> _adapter;
    private CompositionResultAdapter _adapter;

    private List<SQLiteHelper.Element> _filter;
    private SQLiteHelper _db;
    //private Pair<Integer, Pair<List<Pair<String, ArrayList<SQLiteHelper.Element>>>, List<Integer>>> _tempResults;
    private FetchResultTask _task = null;

    public static ResultFragment newInstance() {
        ResultFragment fragment = new ResultFragment();
        return fragment;
    }

    public ResultFragment() {
    }

    class CompositionResultAdapter extends BaseAdapter {
        Context context;
        List<Pair<String, ArrayList<SQLiteHelper.Element>>> steels = new ArrayList<Pair<String, ArrayList<SQLiteHelper.Element>>>();
        List<Integer> ids = new ArrayList<Integer>();
        private LayoutInflater inflater = null;

        public CompositionResultAdapter(Context pcontext) {
            context = pcontext;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() { return ids.size(); }
        @Override
        public Integer getItem(int position) {
            return ids.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = inflater.inflate(R.layout.result_item, null);
            Pair<String, ArrayList<SQLiteHelper.Element>> al = steels.get(pos);
            ((TextView) vi.findViewById(R.id.result_steel)).setText(al.first);
            String[] cols = {"", "", "", "", ""};
            String delim = "";
            for (SQLiteHelper.Element e : al.second) {
                Pair<Float, Float> d = deviation(e);
                cols[0] += delim + String.format("<b>%s</b>", e.name);
                cols[1] += delim + String.format("%.2f%%", e.min);
                cols[3] += delim + String.format("%.2f%%", e.max);
                if (d == null) {
                    cols[2] += delim;
                    cols[4] += delim;
                } else {
                    cols[2] += delim + String.format("<font color=" + (d.first < 0 ? "red" : "green") + ">%.2f%%</font>", d.first);
                    cols[4] += delim + String.format("<font color=" + (d.second < 0 ? "red" : "green") + ">%.2f%%</font>", d.second);
                }
                delim = "<br/>";
            }
            ((TextView) vi.findViewById(R.id.result_element)).setText(Html.fromHtml(cols[0]));
            ((TextView) vi.findViewById(R.id.result_min)).setText(Html.fromHtml(cols[1]));
            ((TextView) vi.findViewById(R.id.result_mindiff)).setText(Html.fromHtml(cols[2]));
            ((TextView) vi.findViewById(R.id.result_max)).setText(Html.fromHtml(cols[3]));
            ((TextView) vi.findViewById(R.id.result_maxdiff)).setText(Html.fromHtml(cols[4]));
            return vi;
        }
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
        _fetchPosition = 0;
    }

    private class FetchResultTask extends AsyncTask<List<SQLiteHelper.Element>, Void, Pair<Integer, Pair<List<Pair<String, ArrayList<SQLiteHelper.Element>>>, List<Integer>>> > {
        @Override
        protected Pair<Integer, Pair<List<Pair<String, ArrayList<SQLiteHelper.Element>>>, List<Integer>>> doInBackground(List<SQLiteHelper.Element>... params) {
            Pair<Integer, Pair<List<Pair<String, ArrayList<SQLiteHelper.Element>>>, List<Integer>>> results = null;
            Log.i("RESULTs", "fetch");
            List<SQLiteHelper.Element> filter = params[0];
            if (_db != null)
                results = _db.fetchSteelComposition(_fetchPosition, FETCH_LENGTH, filter);
            return results;
        }
        protected void onPostExecute(Pair<Integer, Pair<List<Pair<String, ArrayList<SQLiteHelper.Element>>>, List<Integer>>> results) {
            // data fetched
            if (results != null && results.first > _fetchPosition) {
                _fetchPosition = results.first;
                _adapter.steels.addAll(results.second.first);
                _adapter.ids.addAll(results.second.second);
                _adapter.notifyDataSetChanged();
            } else {
                _fetchEnd = true;
            }
            _task = null;
        }
    }

    public void search(List<SQLiteHelper.Element> l, SQLiteHelper db) {
        cancel();
        if (_adapter != null) {
            _adapter.steels.clear();
            _adapter.ids.clear();
        }
        _fetchPosition = 0;
        _filter = l;
        _db = db;
        fetch();
    }
    private Pair<Float, Float> deviation(SQLiteHelper.Element e) {
        for (int i = 0; i < _filter.size(); i++) {
            SQLiteHelper.Element f = _filter.get(i);
            if (f.name.equals(e.name)) {
                float min = e.min - f.min;
                float max = f.max - e.max;
                return Pair.create(min == 0 ? 0 : min * 100 / f.min, max == 0 ? 0 : max * 100 / f.max);
            }
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);
        _context = (MainActivity) getActivity();
        ListView listview = (ListView) view.findViewById(R.id.listView);
        _adapter = new CompositionResultAdapter(_context);
        listview.setAdapter(_adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                _context.viewSteel(_adapter.getItem(position));
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private final int PADDING = 32;
            private int _firstVisibleItem = 0;
            private int _visibleItemCount = 0;
            private int _totalItemCount = 0;
            //private int _scrollState = SCROLL_STATE_IDLE;

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                _firstVisibleItem = firstVisibleItem;
                _visibleItemCount = visibleItemCount;
                _totalItemCount = totalItemCount;
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //_scrollState = scrollState;
                if (_firstVisibleItem + _visibleItemCount + PADDING > _totalItemCount)
                    onScrollCompleted();
            }

            private void onScrollCompleted() {
                fetch();
            }
        });
    }

    public void fetch()
    {
        if (!_fetchEnd && _task == null)
            _task = (FetchResultTask) new FetchResultTask().execute(_filter);
    }
    public void cancel() {
        if (_task != null) {
            _task.cancel(true);
            _task = null;
        }
        _fetchEnd = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancel();
    }
}
