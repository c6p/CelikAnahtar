package com.gulaghad.test.app;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TabHost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SteelFragment extends Fragment {
    private static final String[] _tags = {"composition", "mechanical", "physical", "heat"};
    private int _steel;
    private MainActivity _context;
    private TabChangeListener _listener;
    private AsyncTask<Integer, Void, String>[] _tasks = new AsyncTask[4];

    private static class TabChangeListener implements TabHost.OnTabChangeListener {
        private Map<String, String> _contents;
        private final WebView _view;
        private String _tabId;
        private final String _css = "<style type='text/css'>"
                + "body { color:#fff; background-color:#000; }"
                + "table { width: 100%; }"
                + "tr.standard { background-color:#01579b; }"
                + "tr.state { background-color:#212121; }"
                + "tr.property { background-color:#263238; }"
                + "td.right { text-align:right; }"
                + "td.value { white-space:nowrap; }"
                + "span.unit { font-size:80%; color:#ccc; }"
                + "</style>";
        private final String _loading = "<h3>Loading...</h3>";
        public void onTabChanged(String tabId){
            _tabId = tabId;
            Log.i("tabchange", tabId);
            String content = _contents.get(tabId);
            _view.loadDataWithBaseURL(null, _css + (content == null ? _loading : content),
                    "text/html", "utf-8", null);
        }

        public void initContent() {
            _contents = new HashMap<String, String>();
        }
        public void loadContent(String id, String content) {
            _contents.put(id, content);
            if (id.equals(_tabId))
                _view.loadDataWithBaseURL(null, _css+content, "text/html", "utf-8", null);
        }
        public TabChangeListener(WebView view) {
            _view = view;
            initContent();
        }
    }

    public static SteelFragment newInstance() {
        SteelFragment fragment = new SteelFragment();
        return fragment;
    }
    public SteelFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_steel, container, false);
        _context = (MainActivity) getActivity();
        Bundle args = getArguments();
        if (args != null) {
            _steel = args.getInt("STEEL");
        }
        Log.i("SteelFragment", Integer.toString(_steel));
        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.setBackgroundColor(0);
        _listener = new TabChangeListener(webView);
        fetchData();
        TabHost tabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        tabHost.setup();
        tabHost.setOnTabChangedListener(_listener);
        tabHost.addTab(tabHost.newTabSpec(_tags[0]).setIndicator("Composition").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[1]).setIndicator("Mechanical Properties").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[2]).setIndicator("Physical Properties").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[3]).setIndicator("Heat Treatment").setContent(R.id.webView));
        tabHost.getCurrentView().setVisibility(View.VISIBLE);
        return tabHost;
    }

    // format
    private static String f(float f)
    {
        if(f == (int) f)
            return String.format("%d",(int)f);
        else
            return String.format("%s",f);
    }
    // join
    private static String j(float min, float max) {
        if (min == max) {
            if (min == 0.f) // && max == 0.f
                return "";
            else
                return "~ " + f(min);
        }
        else if (min == 0.f && max != 0)
            return "<=" + f(max);
        else if (max == 0.f && min != 0)
            return ">=" + f(min);
        else // min != max != 0
            return f(min) + " - " + f(max);
    }

    private void fetchData() {
        _listener.initContent();
        _tasks[0] = new FetchCompositionTask().execute(_steel);
        _tasks[1] = new FetchMechanicalPropsTask().execute(_steel);
        _tasks[2] = new FetchHeatTreatTask().execute(_steel);
        _tasks[3] = new FetchPhysicalPropsTask().execute(_steel);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        for (AsyncTask<Integer, Void, String> t : _tasks) {
            if (t != null)
                t.cancel(true);
        }
    }

    private class FetchCompositionTask extends AsyncTask<Integer, Void, String> {
        @Override
        protected String doInBackground(Integer... params) {
            int steel = params[0];
            SQLiteHelper db = _context.getDB();
            List<SQLiteHelper.Element> composition = db.fetchComposition(steel);
            String html = "<table><tr class='heading'><th>Element</th><th>Min %</th><th>Max %</th></tr>";
            for (SQLiteHelper.Element c : composition) {
                html += String.format("<tr><td>%s</td><td class='right'>%.2f</td><td class='right'>%.2f</td></tr>", c.name, c.min, c.max);
            }
            html += "</table>";
            return html;
        }
        protected void onPostExecute(String result) {
            _listener.loadContent(_tags[0], result);
        }
    }
    private class FetchMechanicalPropsTask extends AsyncTask<Integer, Void, String> {
        @Override
        protected String doInBackground(Integer... params) {
            int steel = params[0];
            SQLiteHelper db = _context.getDB();
            List<SQLiteHelper.MechanicalProps> props = db.fetchMechanicalProps(steel);
            String html = "<table><tr class='heading'><th>Dimension</th><th colspan='2'>Value</th>"
                    + "<th>Position of Specimen</th><th>At Temperature</th></tr>";
            String standard = "NULL", state = "NULL", property = "NULL";
            for (SQLiteHelper.MechanicalProps p : props) {
                boolean neStandard = !standard.equals(p.standard);
                boolean neState = !state.equals(p.state);
                boolean neProperty = !property.equals(p.property);
                if (neStandard) {
                    standard = p.standard;
                    if (TextUtils.isEmpty(standard))
                        html += String.format("<tr class='standard' ><td colspan='5'>%s</td></tr>",
                                p.title);
                    else
                        html += String.format("<tr class='standard'><td colspan='5'>%s (%s)<br/>%s</td></tr>",
                                standard, p.date, p.title);

                }
                if (neState || neStandard) {
                    state = p.state;
                    html += String.format("<tr class='state'><td colspan='5'>%s</td></tr>", state);
                }
                if (neProperty || neState || neStandard) {
                    property = p.property;
                    html += String.format("<tr class='property'><td colspan='5'>%s</td></tr>", property);
                }
                String t = j(p.atTempMin, p.atTempMax);
                html += String.format("<tr><td class='right'>%s</td><td class='value right'>%s </td>"
                                + "<td><span class='unit'>%s</span'></td><td>%s</td><td class='right'>%s</td></tr>",
                        j(p.dimMin, p.dimMax), j(p.valMin, p.valMax), p.unit, p.sampleLoc,
                        t.equals("") ? "" : t + "<span class='unit'> °C</span'>"
                );
            }
            html += "</table>";
            return html;
        }
        protected void onPostExecute(String result) {
            _listener.loadContent(_tags[1], result);
        }
    }
    private class FetchPhysicalPropsTask extends AsyncTask<Integer, Void, String> {
        @Override
        protected String doInBackground(Integer... params) {
            int steel = params[0];
            SQLiteHelper db = _context.getDB();
            List<SQLiteHelper.PhysicalProps> props = db.fetchPhysicalProps(steel);
            String html = "<table><tr class='heading'><th colspan='2'>Value</th><th>At Temperature</th></tr>";
            String standard = "NULL", state = "NULL", property = "NULL";
            for (SQLiteHelper.PhysicalProps p : props) {
                boolean neStandard = !standard.equals(p.standard);
                boolean neState = !state.equals(p.state);
                boolean neProperty = !property.equals(p.property);
                if (neStandard) {
                    standard = p.standard;
                    if (TextUtils.isEmpty(standard))
                        html += String.format("<tr class='standard' ><td colspan='3'>%s</td></tr>",
                                p.title);
                    else
                        html += String.format("<tr class='standard'><td colspan='3'>%s (%s)<br/>%s</td></tr>",
                                standard, p.date, p.title);

                }
                if (neState || neStandard) {
                    state = p.state;
                    html += String.format("<tr class='state'><td colspan='3'>%s</td></tr>", state);
                }
                if (neProperty || neState || neStandard) {
                    property = p.property;
                    html += String.format("<tr class='property'><td colspan='3'>%s</td></tr>", property);
                }
                String t = j(p.tempMin, p.tempMax);
                html += String.format("<tr><td class='value right'>%s </td>"
                                + "<td><span class='unit'>%s</span'></td><td class='right'>%s</td></tr>",
                        p.classification == null ? j(p.valMin, p.valMax) : p.classification,
                        p.unit, t.equals("") ? "" : t + "<span class='unit'> °C</span'>"
                );
            }
            html += "</table>";
            return html;
        }
        protected void onPostExecute(String result) {
            _listener.loadContent(_tags[2], result);
        }
    }
    private class FetchHeatTreatTask extends AsyncTask<Integer, Void, String> {
        @Override
        protected String doInBackground(Integer... params) {
            int steel = params[0];
            SQLiteHelper db = _context.getDB();
            List<SQLiteHelper.HeatTreat> props = db.fetchHeatTreat(steel);
            String html = "<table><tr class='heading'><th colspan='2'>Value</th><th>Cooling</th></tr>";
            String standard = "NULL", state = "NULL", property = "NULL";
            for (SQLiteHelper.HeatTreat p : props) {
                boolean neStandard = !standard.equals(p.standard);
                boolean neState = !state.equals(p.state);
                boolean neProperty = !property.equals(p.property);
                if (neStandard) {
                    standard = p.standard;
                    if (TextUtils.isEmpty(standard))
                        html += String.format("<tr class='standard' ><td colspan='3'>%s</td></tr>",
                                p.title);
                    else
                        html += String.format("<tr class='standard'><td colspan='3'>%s (%s)<br/>%s</td></tr>",
                                standard, p.date, p.title);

                }
                if (neState || neStandard) {
                    state = p.state;
                    html += String.format("<tr class='state'><td colspan='3'>%s</td></tr>", state);
                }
                if (neProperty || neState || neStandard) {
                    property = p.property;
                    html += String.format("<tr class='property'><td colspan='3'>%s</td></tr>", property);
                }
                html += String.format("<tr><td class='value right'>%s </td>"
                                + "<td><span class='unit'>%s</span'></td><td>%s</td></tr>",
                        j(p.valMin, p.valMax), p.unit, p.cooling
                );
            }
            html += "</table>";
            return html;
        }
        protected void onPostExecute(String result) {
            _listener.loadContent(_tags[3], result);
        }
    }

}
