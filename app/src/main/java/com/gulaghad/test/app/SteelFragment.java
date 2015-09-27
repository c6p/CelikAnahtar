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
import android.widget.HorizontalScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import java.util.List;


public class SteelFragment extends Fragment {
    private static final String[] _tags = {"info", "composition", "mechanical", "physical", "heat"};
    private TabChangeListener _listener;
    TabHost tabHost;

    private class CustomGestureDetector extends GestureDetector.SimpleOnGestureListener {

        public void centerTabItem(int position) {
            tabHost.setCurrentTab(position);
            final TabWidget tabWidget = tabHost.getTabWidget();

            final int screenWidth = tabHost.getWidth();
            final int leftX = tabWidget.getChildAt(position).getLeft();
            int newX = 0;

            newX = leftX + (tabWidget.getChildAt(position).getWidth() / 2) - (screenWidth / 2);
            if (newX < 0) {
                newX = 0;
            }
            ((HorizontalScrollView) tabHost.findViewById(R.id.horScrollView)).scrollTo(newX, 0);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            ;
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            else {
                int tab = tabHost.getCurrentTab();
                try { // right to left swipe .. go to next page
                    if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 800) {
                        tabHost.setCurrentTab(++tab);
                        centerTabItem(tab);
                        return true;
                    } //left to right swipe .. go to prev page
                    else if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 800) {
                        tabHost.setCurrentTab(--tab);
                        centerTabItem(tab);
                        return true;
                    }
                } catch (Exception e) { // nothing
                }
                return false;
            }
        }
    }

    private static class TabChangeListener implements TabHost.OnTabChangeListener,
            IDataRequester<SQLiteHelper.PropertyList> {
        private WebView _view;
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
        private final String _loading = "<h3 class='centered'>Loading...</h3>";
        private final String _noData = "<h3 class='centered'>No Data</h3>";

        private String _tabId;

        public TabChangeListener() {
            Register.propertyListDataRequester.set(this);
        }

        public void onTabChanged(String tabId) {
            _tabId = tabId;
            Log.i("tabchange", tabId);
            displayContent();
        }

        // format
        private String f(float f) {
            if (f == (int) f)
                return String.format("%d", (int) f);
            else
                return String.format("%s", f);
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

        private String htmlI(List<SQLiteHelper.Description> info) {
            if (info == null)
                return null;
            if (info.isEmpty())
                return _noData;
            String html = "<table>";
            for (SQLiteHelper.Description i : info) {
                html += String.format("<tr><td>Material</td><td>%s</td></tr>", i.wsnr);
                html += String.format("<tr><td>Symbol</td><td>%s</td></tr>", i.designation);
                html += String.format("<tr><td>Name</td><td>%s</td></tr>", i.names);
                html += String.format("<tr><td>Steel Group</td><td>%s</td></tr>", i.group);
                html += String.format("<tr><td>Standard</td><td>%s</td></tr>", i.standard);
            }
            html += "</table>";
            return html;
        }

        private String htmlC(List<SQLiteHelper.Element> composition) {
            if (composition == null)
                return null;
            if (composition.isEmpty())
                return _noData;
            String html = "<table><tr class='heading'><th>Element</th><th>Min %</th><th>Max %</th></tr>";
            for (SQLiteHelper.Element c : composition) {
                html += String.format("<tr><td>%s</td><td class='right'>%.2f</td><td class='right'>%.2f</td></tr>", c.name, c.min, c.max);
            }
            html += "</table>";
            return html;
        }

        private String htmlMP(List<SQLiteHelper.MechanicalProp> props) {
            if (props == null)
                return null;
            if (props.isEmpty())
                return _noData;
            String html = "<table><tr class='heading'><th>Dimension</th><th colspan='2'>Value</th>"
                    + "<th>Position of Specimen</th><th>At Temperature</th></tr>";
            String standard = "NULL", state = "NULL", property = "NULL";
            for (SQLiteHelper.MechanicalProp p : props) {
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

        private String htmlPP(List<SQLiteHelper.PhysicalProp> props) {
            if (props == null)
                return null;
            if (props.isEmpty())
                return _noData;
            String html = "<table><tr class='heading'><th colspan='2'>Value</th><th>At Temperature</th></tr>";
            String standard = "NULL", state = "NULL", property = "NULL";
            for (SQLiteHelper.PhysicalProp p : props) {
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

        private String htmlHT(List<SQLiteHelper.HeatTreat> props) {
            if (props == null)
                return null;
            if (props.isEmpty())
                return _noData;
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

        private void displayContent() {
            if (_view == null)
                return;
            IDataProvider<SQLiteHelper.PropertyList> provider = Register.propertyListDataProvider.get();
            if (provider == null)
                return;

            String content = null;
            SQLiteHelper.PropertyList steel = provider.getData();
            if (steel != null) {
                Log.i(_tabId, steel.toString());
                if (_tabId == "info")
                    content = htmlI(steel.info);
                if (_tabId == "composition")
                    content = htmlC(steel.composition);
                else if (_tabId == "mechanical")
                    content = htmlMP(steel.mechanicalProps);
                else if (_tabId == "physical")
                    content = htmlPP(steel.physicalProps);
                else if (_tabId == "heat")
                    content = htmlHT(steel.heatTreats);
            } else {
                content = _noData;
            }

            if (content == null)
                content = _loading;
            _view.loadDataWithBaseURL(null, _css + content, "text/html", "utf-8", null);
        }

        public void setView(WebView view) {
            _view = view;
            displayContent();
        }

        @Override
        public void dataReady() {
            Log.i("TabListener", "DATA READY");
            displayContent();
        }
    }

    public static SteelFragment newInstance() {
        SteelFragment fragment = new SteelFragment();
        return fragment;
    }

    public SteelFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstaceState) {
        savedInstaceState.putInt("tabId", tabHost.getCurrentTab());
        super.onSaveInstanceState(savedInstaceState);
    }

    @Override
    public void onAttach(Activity activity) {
        _listener = new TabChangeListener();
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_steel, container, false);
        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.setBackgroundColor(0);
        webView.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector((MainActivity) getActivity(), new CustomGestureDetector());

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });
        tabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        tabHost.setup();
        tabHost.setOnTabChangedListener(_listener);
        tabHost.addTab(tabHost.newTabSpec(_tags[0]).setIndicator("Info").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[1]).setIndicator("Composition").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[2]).setIndicator("Mechanical Properties").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[3]).setIndicator("Physical Properties").setContent(R.id.webView));
        tabHost.addTab(tabHost.newTabSpec(_tags[4]).setIndicator("Heat Treatment").setContent(R.id.webView));
        for (int i = 0; i < tabHost.getTabWidget().getTabCount(); i++) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setAllCaps(false);
            tv.setTextSize(10);
            tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 60;
        }
        tabHost.getCurrentView().setVisibility(View.VISIBLE);
        _listener.setView(webView);
        if (savedInstanceState != null)
            tabHost.setCurrentTab(savedInstanceState.getInt("tabId"));
        return tabHost;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}