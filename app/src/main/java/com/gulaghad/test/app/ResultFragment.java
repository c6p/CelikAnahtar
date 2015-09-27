package com.gulaghad.test.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ResultFragment extends Fragment implements IDataRequester<SQLiteHelper.SteelPropertyList> {
    private CompositionResultAdapter _adapter;
    private boolean _loadingIndicator = true;

    public static ResultFragment newInstance() {
        ResultFragment fragment = new ResultFragment();
        return fragment;
    }

    public ResultFragment() {
    }

    @Override
    public void dataReady() {
        if (_adapter == null)
            return;
        Log.i("property", "dataREADY");
        _adapter.setData();
    }

    class CompositionResultAdapter extends BaseAdapter {
        private LayoutInflater inflater = null;
        private SQLiteHelper.SteelPropertyList data;

        public CompositionResultAdapter(LayoutInflater li) {
            inflater = li;
        }

        public void setData() {
            IDataProvider<SQLiteHelper.SteelPropertyList> provider = Register.steelPropertyListDataProvider.get();
            if (provider == null) {
                data = null;
                return;
            }
            data = provider.getData();
            _showLoadingIndicator(false);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (data == null)
                return 0;
            return data.steelIds.size();
        }
        @Override
        public Integer getItem(int position) {
            if (data == null)
                return 0;
            return data.steelIds.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        // format
        private String f(float f)
        {
            if(f == (int) f)
                return String.format("%d",(int)f);
            else
                return String.format("%s",f);
        }
        // join
        private String j(float min, float max) {
            if (min == max) {
                if (min == 0.f) // && max == 0.f
                    return "";
                else
                    return "~ " + f(min);
            } else if (min == 0.f && max != 0)
                return "<=" + f(max);
            else if (max == 0.f && min != 0)
                return ">=" + f(min);
            else // min != max != 0
                return f(min) + " - " + f(max);
        }

        public String[] stringElements(List<SQLiteHelper.Element> elements, List<SQLiteHelper.Element> filter) {
            String[] cols = {"", "", "", "", ""};
            String delim = "";
            for (SQLiteHelper.Element e : elements) {
                Pair<Float, Float> d = deviation(e, filter);
                cols[0] += delim + String.format("<b>%s</b>", e.name);
                cols[1] += delim + String.format("%.2f%%", e.min);
                cols[3] += delim + String.format("%.2f%%", e.max);
                if (d == null) {
                    cols[2] += delim;
                    cols[4] += delim;
                } else {
                    Pair<String, String> cMin = colorString(d.first);
                    Pair<String, String> cMax = colorString(d.second);
                    cols[2] += delim + "<font color=" + cMin.second + ">" + cMin.first + "</font>";
                    cols[4] += delim + "<font color=" + cMax.second + ">" + cMax.first + "</font>";
                }
                delim = "<br/>";
            }
            return cols;
        }

        public String[] stringProperties(List<SQLiteHelper.SearchProperty> properties, List<SQLiteHelper.SearchProperty> filter) {
            String[] cols = {"", "", "", "", ""};
            String delim = "";
            for (SQLiteHelper.SearchProperty p : properties) {
//                Pair<Float, Float> d = deviation(p, filter);
                cols[0] += delim + String.format("<b>%s</b>", p.property);
                cols[1] += delim + String.format("%s", j(p.value_min, p.value_max));
                cols[2] += delim + String.format("%s mm", j(p.dim_min, p.dim_max));
                cols[3] += delim + String.format("%s °C", j(p.temp_min, p.temp_max));
                cols[4] += delim + p.state;
                delim = "<br/>";
            }
            return cols;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = inflater.inflate(R.layout.result_item, null);

            setData();
            if (data == null)
                return vi;

            ((TextView) vi.findViewById(R.id.result_steel)).setText(data.steelNames.get(pos));

            SQLiteHelper.SteelPropertyList.Prop filter = data.filter;
            SQLiteHelper.SteelPropertyList.Prop props = data.steelProps.get(pos);
            String[] elem = stringElements(props.elements, filter.elements);
            String[] prop = stringProperties(props.properties, filter.properties);

            ((TextView) vi.findViewById(R.id.result_element)).setText(Html.fromHtml(elem[0]+prop[0]));
            ((TextView) vi.findViewById(R.id.result_min)).setText(Html.fromHtml(elem[1]+prop[1]));
            ((TextView) vi.findViewById(R.id.result_mindiff)).setText(Html.fromHtml(elem[2]+prop[2]));
            ((TextView) vi.findViewById(R.id.result_max)).setText(Html.fromHtml(elem[3]+prop[3]));
            ((TextView) vi.findViewById(R.id.result_maxdiff)).setText(Html.fromHtml(elem[4]+prop[4]));
            return vi;
        }
    }
    private void _showLoadingIndicator(boolean show) {
        _loadingIndicator = show;
        if (getView() != null)
            getView().findViewById(R.id.result_loading).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new CompositionResultAdapter((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        Register.steelPropertyListDataRequester.set(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private Pair<Float, Float> deviation(SQLiteHelper.Element e, List<SQLiteHelper.Element> filter) {
        if (filter == null || e == null)
            return null;
        for (int i = 0; i < filter.size(); i++) {
            SQLiteHelper.Element f = filter.get(i);
            if (f.name.equals(e.name)) {
                float min = (e.min - f.min) * 100 / f.min;
                float max = (f.max - e.max) * 100 / f.max;
                return Pair.create(min, max);
            }
        }
        return null;
    }

    private Pair<String, String> colorString(Float f) {
        if (Float.isInfinite(f) || Float.isNaN(f))
            return Pair.create("-", "gray");
        else if (f == 0)
            return Pair.create("0", "gray");
        else {
            String s = String.format("%.2f", f);
            if (f < 0)
                return Pair.create(s, "red");
            else // f > 0
                return Pair.create(s, "green");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);
        ListView listview = (ListView) view.findViewById(R.id.listView);
        listview.setAdapter(_adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<Integer> ids = _adapter.data.steelIds;
                OnSteelSelectedListener _steelListener = (OnSteelSelectedListener) getActivity();
                for (int i = 0; i < _adapter.data.steelIds.size(); i++)
                    Log.i("RecentFragment", "id_steel" + i + ":" + ids.get(i));
                _steelListener.onSteelSelected(ids.get(position));
            }
//                _context.viewSteel(_adapter.getItem(position));

        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = (ListView) view.findViewById(R.id.listView);
        _showLoadingIndicator(_loadingIndicator);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
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
                if (_firstVisibleItem + _visibleItemCount + PADDING > _totalItemCount)
                    onScrollCompleted();
            }

            private void onScrollCompleted() {

            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
