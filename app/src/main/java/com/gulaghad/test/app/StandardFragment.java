package com.gulaghad.test.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class StandardFragment extends Fragment
        implements IDataRequester<SQLiteHelper.StandardSteelList> {
    private OnSteelSelectedListener _steelListener;
    private WebView _webView;
    private ListView _listView;
    private final List<String> _steels = new ArrayList<String>();
    private final List<Integer> _ids = new ArrayList<Integer>();
    private ArrayAdapter<String> _adapter;
    private final String _css = "<style type='text/css'>"
            + "body { color:#fff; background-color:#000; }"
            + "table { width: 100%; }"
            + "tr.standard { background-color:#01579b; }"
            + "tr.state { background-color:#212121; }"
            + "tr.property { background-color:#263238; }"
            + "td.right { text-align:right; }"
            + "td.value { white-space:nowrap; }"
            + "span.unit { font-size:80%; color:#ccc; }"
            + ".centered { position:absolute; text-align:center; top:40%; width:95% }"
            + "</style>";

    private String html(SQLiteHelper.Standard standard) {
        String html = _css + "<table>";
        html += String.format("<tr><td>Standard</td><td>%s</td></tr>", standard.name);
        html += String.format("<tr><td>Title</td><td>%s</td></tr>", standard.title);
        html += "</table>";
        return html;
    }

    @Override
    public void dataReady() {
        Log.i("DATA", "ready");
        renderStandard();
        loadSteels();
    }
    private void renderStandard() {
        if (_webView == null)
            return;
        _webView.loadDataWithBaseURL(null, html(Register.standard), "text/html", "utf-8", null);
    }

    private void loadSteels() {
        if (_listView == null) return;
        IDataProvider<SQLiteHelper.StandardSteelList> provider = Register.standardSteelListDataProvider.get();
        if (provider == null) return;
        SQLiteHelper.StandardSteelList data = provider.getData();
        _steels.clear();
        _ids.clear();
        if (data != null) {
            assert data.steelNames.size() == data.steelIds.size();
            _steels.addAll(data.steelNames);
            _ids.addAll(data.steelIds);
        }
        _adapter.notifyDataSetChanged();
    }
    public static StandardFragment newInstance() {
        StandardFragment fragment = new StandardFragment();
        return fragment;
    }

    public StandardFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _steelListener = (OnSteelSelectedListener) activity;
        _adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, _steels);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Register.standardSteelListDataRequester.set(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_standard, container, false);
        _webView = (WebView) view.findViewById(R.id.webView);
        _webView.setBackgroundColor(0);
        _listView = (ListView) view.findViewById(R.id.listView);
        _listView.setAdapter(_adapter);
        _listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> l, View view, int position, long id) {
                _steelListener.onSteelSelected(_ids.get(position));
            }
        });
        renderStandard();
        loadSteels();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Register.standardSteelListDataRequester.unset(this);
        super.onDestroy();
    }
}