package com.gulaghad.test.app;

import java.io.Serializable;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ActionBar;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import static android.widget.SearchView.OnQueryTextListener;
import static com.gulaghad.test.app.SQLiteHelper.*;
import static com.gulaghad.test.app.PropertyType.*;


//enum DataRequestType {
//    NoData,
//    Steel,
//    SteelsByName,
//    SteelsByProperty,
//    SteelsInComparison,
//    Composition,
//    MechanicalProps,
//    PhysicalProps,
//    HeatTreatment
//}
//enum DataRequestType {
//    SteelsByName,
//    SteelsByProperty,
//    SteelProps,
//    SteelPropsComparision
//}


//interface IDataHandler<T> {
//    public void handle(T data);
//}

abstract class FetchTask<T, S> extends AsyncTask<T, Void, S> {
    private IDataHandler<S> _handler;
    private SQLiteHelper _db;

    public FetchTask(SQLiteHelper db, IDataHandler<S> handler) {
        this._db = db;
        this._handler = handler;
    }
    protected SQLiteHelper db() { return _db; }
    protected void onPostExecute(S result) { _handler.setData(result); }
}

class FetchSteelTask extends FetchTask<String, SQLiteHelper.SteelList> {
    private final int FETCH_LENGTH = 32;
    private int _fetchPosition;
    public FetchSteelTask(SQLiteHelper db, IDataHandler handler, int position) {
        super(db, handler);
        _fetchPosition = position;
    }

    @Override
    protected SQLiteHelper.SteelList doInBackground(String... params) {
        String filter = params[0];
        if (!filter.isEmpty()) {
            Log.i(this.toString(), filter);
            SQLiteHelper.SteelList results = db().fetchSteel(_fetchPosition, FETCH_LENGTH, filter);
            Log.i(this.toString(), results.toString());
            return results;
        }
        return null;
    }
}

class FetchPropertyTask extends FetchTask<Integer, Pair<PropertyType, List>> {
    private final PropertyType _type;

    public FetchPropertyTask(SQLiteHelper db, IDataHandler<Pair<PropertyType, List>> handler, PropertyType type) {
        super(db, handler);
        _type = type;
    }

    protected Pair<PropertyType, List> doInBackground(Integer... params) {
        int steel = params[0];
        List props = db().fetchSteelProps(steel, _type);
        return new Pair<PropertyType, List>(_type, props);
    }
}

class FetchSearchResultTask extends FetchTask<SteelPropertyList.Prop, SteelPropertyList> {
    private final int FETCH_LENGTH = 256;
    private int _fetchPosition;

    public FetchSearchResultTask(SQLiteHelper db, IDataHandler<SteelPropertyList> handler, int position) {
        super(db, handler);
        _fetchPosition = position;
    }

    @Override
    protected SteelPropertyList doInBackground(SteelPropertyList.Prop... params) {
        SteelPropertyList.Prop prop = params[0];
        if ( prop == null || (prop.elements == null || prop.elements.isEmpty()) &&
                (prop.properties == null || prop.properties.isEmpty()) )
            return null;
        SteelPropertyList list = db().fetchSteelProperty(_fetchPosition, FETCH_LENGTH, prop);
        return list;
    }
}

//class FetchCompositionTask extends FetchTask<Integer, List<Element>> {
//    public FetchCompositionTask(SQLiteHelper db, IDataHandler<List<Element>> handler) {
//        super(db, handler);
//    }
//
//    protected List<Element> doInBackground(Integer... params) {
//        int steel = params[0];
//        List<SQLiteHelper.Element> props = db().fetchComposition(steel);
//        return props;
//    }
//}
//
//class FetchMechanicalPropsTask extends FetchTask<Integer, List<SQLiteHelper.MechanicalProp>> {
//    public FetchMechanicalPropsTask(SQLiteHelper db, IDataHandler<List<SQLiteHelper.MechanicalProp>> handler) {
//        super(db, handler);
//    }
//
//    protected List<SQLiteHelper.MechanicalProp> doInBackground(Integer... params) {
//        int steel = params[0];
//        List<SQLiteHelper.MechanicalProp> props = db().fetchMechanicalProps(steel);
//        return props;
//    }
//}
//
//class FetchPhysicalPropsTask extends FetchTask<Integer, List<SQLiteHelper.PhysicalProp>> {
//    public FetchPhysicalPropsTask(SQLiteHelper db, IDataHandler<List<SQLiteHelper.PhysicalProp>> handler) {
//        super(db, handler);
//    }
//
//    protected List<SQLiteHelper.PhysicalProp> doInBackground(Integer... params) {
//        int steel = params[0];
//        List<SQLiteHelper.PhysicalProp> props = db().fetchPhysicalProps(steel);
//        return props;
//    }
//}
//
//class FetchHeatTreatTask extends FetchTask<Integer, List<SQLiteHelper.HeatTreat>> {
//    public FetchHeatTreatTask(SQLiteHelper db, IDataHandler<List<SQLiteHelper.HeatTreat>> handler) {
//        super(db, handler);
//    }
//
//    protected List<SQLiteHelper.HeatTreat> doInBackground(Integer... params) {
//        int steel = params[0];
//        List<SQLiteHelper.HeatTreat> props = db().fetchHeatTreat(steel);
//        return props;
//    }
//}

class PropertySearch implements IDataProvider<SteelPropertyList> {
    private SQLiteHelper _db;
    private FetchSearchResultTask _task = null;

    PropertySearch(SQLiteHelper db) {
        assert db != null;
        this._db = db;
        Register.steelPropertyListDataProvider.set(this);
        IDataRequester<SteelPropertyList> requester = Register.steelPropertyListDataRequester.get();
        if (requester != null)
            requester.dataReady();

    }

    @Override
    public void requestData(int size) {
        if (Cache.steelPropertyList != null) {
            SQLiteHelper.SteelPropertyList data = Cache.steelPropertyList;
            if (!data.complete && size >= data.position) {
                _fetch(data.position);
            }
            IDataRequester<SQLiteHelper.SteelList> requester = Register.steelListDataRequester.get();
            if (requester != null)
                requester.dataReady();
        } else {
            _fetch(0);
        }
    }
    private void _fetch(int fetchPosition) {
        if (Register.propertyFilter != null && _task == null)
            _task = (FetchSearchResultTask) new FetchSearchResultTask(_db, this, fetchPosition).execute(Register.propertyFilter);
    }
    private void _cancel() {
        if (_task != null) {
            _task.cancel(true);
            _task = null;
        }
    }

    public void search(SteelPropertyList.Prop p) {
        if (Register.propertyFilter == p) {
            return;
        }
        _cancel();
        Register.propertyFilter = p;
        Cache.steelPropertyList = null;
        requestData(0);
    }

    @Override
    public SteelPropertyList getData() {
        return Cache.steelPropertyList;
    }

    @Override
    public void setData(SteelPropertyList data) {
        Log.i("setdata", "1");
        if (data == null)
            return;
        Log.i("setdata", "2");
        if (Cache.steelPropertyList != null) {
            // merge data
            SQLiteHelper.SteelPropertyList cache = Cache.steelPropertyList;
            if (cache.position < data.position) {
                cache.steelProps.addAll(data.steelProps);
                cache.steelIds.addAll(data.steelIds);
                cache.complete = data.complete;
                cache.position = data.position;
            }
        } else
            Cache.steelPropertyList = data;

        IDataRequester<SteelPropertyList> requester = Register.steelPropertyListDataRequester.get();
        if (requester != null)
            requester.dataReady();

        Log.i("setdata - size", Integer.toString(data.steelIds.size()));

        _task = null;
    }
}

class SteelSearch implements IDataProvider<SQLiteHelper.SteelList> {
//    private IDataNegotiator<SQLiteHelper.SteelList> _negotiator;
    private SQLiteHelper _db;
//    private String _filter = new String();
    //private int _fetchPosition = 0;
    //private boolean _fetchEnd = false;
    private FetchSteelTask _task = null;
//    private static TreeMap<String, SQLiteHelper.SteelList> _cachedFilters = new TreeMap<String, SQLiteHelper.SteelList>();

    SteelSearch(SQLiteHelper db, MainActivity ma)
    {
        assert db != null;
        this._db = db;

//        _negotiator = Negotiator.get(SQLiteHelper.SteelList.class);
//        _negotiator.register(this);
        Register.steelListDataProvider.set(this);

//        IDataRequester<SQLiteHelper.SteelList> requester = Register.steelListDataRequester.get();
//        if (requester != null)
//            requester.dataReady();
    }

//    public String filter() {
//        return _filter;
//    }

    private void _fetch(int fetchPosition) {
        if (!Register.steelFilter.isEmpty() && _task == null)
            _task = (FetchSteelTask) new FetchSteelTask(_db, this, fetchPosition).execute(Register.steelFilter);
    }
    private void _cancel() {
        if (_task != null) {
            _task.cancel(true);
            _task = null;
        }
    }
    public void search(String s) {
        Log.i("searchView", s);
        if (Register.steelFilter.equals(s)) {
            return;
        }
        _cancel();
        Register.steelFilter = s;
        requestData(0);
    }

    @Override
    public void requestData(int size) {
        if (Cache.steelList.containsKey(Register.steelFilter)) {
            SQLiteHelper.SteelList data = Cache.steelList.get(Register.steelFilter);
            Log.i("requestData", "DATA");
            if (!data.complete && size >= data.position) {
                Log.i("requestData", Integer.toString(data.position));
                _fetch(data.position);
            }
            IDataRequester<SQLiteHelper.SteelList> requester = Register.steelListDataRequester.get();
            if (requester != null)
                requester.dataReady();
        } else {
            _fetch(0);
        }
    }

    @Override
    public SQLiteHelper.SteelList getData() {
        if (Cache.steelList.containsKey(Register.steelFilter)) {
            return Cache.steelList.get(Register.steelFilter);
        }
        return null;
    }

    @Override
    public void setData(SQLiteHelper.SteelList data) {
        if (data == null)
            return;
            if (Cache.steelList.containsKey(Register.steelFilter)) {
                // merge data
                SQLiteHelper.SteelList cache = Cache.steelList.get(Register.steelFilter);
                if (cache.position < data.position) {
                    cache.steelNames.addAll(data.steelNames);
                    cache.steelIds.addAll(data.steelIds);
                    cache.complete = data.complete;
                    cache.position = data.position;
                }
            } else
                Cache.steelList.put(Register.steelFilter, data);

        IDataRequester<SQLiteHelper.SteelList> requester = Register.steelListDataRequester.get();
        if (requester != null)
            requester.dataReady();

        _task = null;
    }
}



class SteelDetails implements IDataProvider<PropertyList> {
    class Handler implements IDataHandler<Pair<PropertyType, List>> {
        private int _steel;
        private FetchPropertyTask[] _tasks = new FetchPropertyTask[PropertyType.values().length];

        public Handler() {
            for (PropertyType sp : PropertyType.values()) {

            }
        }

        public int steel() { return _steel; }

        public void fetch(int steel) {
            Log.i("Handler.fetch", Integer.toString(_steel)+"="+Integer.toString(steel));
            if (_steel == steel)
                return;

            _steel = steel;
            for (PropertyType sp : PropertyType.values()) {
                int i = sp.ordinal();
                if (_tasks[i] != null) {
                    _tasks[i].cancel(true);
                    _tasks[i] = null;
                }
                _tasks[i] = new FetchPropertyTask(_db, this, sp);
                _tasks[i].execute(_steel);
            }
        }

        @Override
        public void setData(Pair<PropertyType, List> data) {
            Log.i("Handler", "setData");
            if (data == null)
                return;

            PropertyList cache;
            if (Cache.propertyList.containsKey(_steel))
                cache = Cache.propertyList.get(_steel);
            else {
                cache = new PropertyList(_steel);
                Cache.propertyList.put(_steel, cache);
            }

            switch (data.first) {
                case Composition:
                    cache.composition = data.second;
                    break;
                case Mechanical:
                    cache.mechanicalProps = data.second;
                    break;
                case Physical:
                    cache.physicalProps = data.second;
                    break;
                case Heat:
                    cache.heatTreats = data.second;
                    break;
            }

            SteelDetails.this.setData(cache);
        }
    }

    private SQLiteHelper _db;
    private Handler _handler = new Handler();

    public SteelDetails(SQLiteHelper db) {
        _db = db;
        Register.propertyListDataProvider.set(this);
    }

    public void view(int steel) {
        Log.i("SteelDetails", "view");
        _handler.fetch(steel);
    }

    @Override
    public void requestData(int size) { }

    @Override
    public PropertyList getData() {
        if (Cache.propertyList.containsKey(_handler.steel())) {
            return Cache.propertyList.get(_handler.steel());
        }
        return null;
    }

    @Override
    public void setData(PropertyList data) {
        IDataRequester<SQLiteHelper.PropertyList> requester = Register.propertyListDataRequester.get();
        if (requester != null)
            requester.dataReady();
    }
}

class FragmentData {
    public String tag;
    public boolean backstack;
    public Fragment fragment;
}

interface OnSteelSelectedListener {
    public void onSteelSelected(int steel);
}
interface OnPropertySearchListener {
    public void onPropertySearch(List data);
}

public class MainActivity extends Activity implements OnSteelSelectedListener, OnPropertySearchListener {
    private SQLiteHelper _db;
//    private Map<Integer, Bundle> _requestData;
//    private Map<Integer, Bundle> _cachedSteels;
//    private int _currentSteelId;
//    private String _currentFilter;
//    private ArrayMap<String, ArrayMap<Integer, String>> _steelGroups;

    private SteelSearch _steelSearch;
    private SteelDetails _steelDetails;
    private PropertySearch _propertySearch;
//    private String _query;
//    private int _steel;
//    private IDataRequester<SQLiteHelper.SteelList> _steelListRequester;
//    private IDataProvider<SQLiteHelper.SteelList> _steelListProvider;

    private FragmentData _activeFragment;
//    public void setData(DataRequestType type, Bundle data) {
//        switch (type) {
//            case SteelsByName: {
//                String filter = data.getString("Filter");
//                if (_cachedFilters.containsKey(filter))
//                    break;
//                _cachedFilters.put(filter, data);
//                break;
//            }
//        }
//    }
//
//    @Override
//    public SQLiteHelper.SteelList getData() {
//        if (_cachedFilters.containsKey(_currentFilter)) {
//            SQLiteHelper.SteelList data = _cachedFilters.get(_currentFilter);
//            if (!data.complete)
//
//            return data;
//        }
//        return null;
//    }

//    class QueryTextListener implements OnQueryTextListener {
////        private final MenuItem _searchItem;
//        private final MainActivity _activity;
//        private String _filter = "";
//        private int _fetchPosition = 0;
//        private boolean _fetchEnd = false;
//        private FetchSteelTask _task = null;
//        private Bundle _data;
//
//        QueryTextListener(MainActivity ma) {
//            _activity = ma;
//        }
//
//        @Override
//        public boolean onQueryTextSubmit(String s) {
////            _searchItem.collapseActionView();
//            return _steelSearch.search(s);
//        }
//        @Override
//        public boolean onQueryTextChange(String s) {
//            return _search(s, false);
//        }
//
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup action bar for tabs
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        _db = SQLiteHelper.get(getApplicationContext());
        _steelSearch = new SteelSearch(_db, this);
        _steelDetails = new SteelDetails(_db);
        _propertySearch = new PropertySearch(_db);
//        register(_steelSearch);
        if (savedInstanceState == null) {
            _activateFragment(SearchFragment.class.getName(), false);
        }
//        else {
//            String filter = savedInstanceState.getString("filter");
            //_steelSearch.search(filter);
//            _query = filter;
//        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//        outState.putString("filter", _query);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
//                searchView.onActionViewCollapsed();
//                Register.queryFocused = false;
//                searchItem.collapseActionView();
                return search(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return search(newText);
            }

            private boolean search(String s) {
//                Register.steelFilter = s;
//                Bundle b = new Bundle();
                //b.putSerializable("NEGOTIATOR", negotiator());
                if (Register.queryFocused)
                    _steelSearch.search(s);
//                _activateFragment(RecentFragment.class.getName(), true);
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean queryTextFocused) {
                Register.queryFocused = queryTextFocused;
                if(!queryTextFocused) {
                    searchItem.collapseActionView();
                } else {
                    searchView.setQuery(Register.steelFilter, true);
                    _activateFragment(RecentFragment.class.getName(), true);
                }
            }
        });
        searchView.setQueryHint(getResources().getString(R.string.hint_search));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
            //case R.id.action_settings:
            //    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    public SQLiteHelper getDB() {
//        return _db;
//    }

    private void _activateFragment(String tag, boolean backstack) {
        FragmentManager fm = getFragmentManager();
        fm.popBackStackImmediate(tag, 0);
//        Fragment fragment = fm.findFragmentByTag(tag);
//        if (fragment == null) {
//            fragment = Fragment.instantiate(MainActivity.this, tag);
////            fragment.setArguments(bundle);
//        }
//        if (!fragment.isVisible()) {
        if (fm.getBackStackEntryCount() < 1 || fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1).getName() != tag) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment == null)
                fragment = Fragment.instantiate(MainActivity.this, tag);
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(android.R.id.content, fragment, tag);
            if (backstack)
                fragmentTransaction.addToBackStack(tag);
            fragmentTransaction.commit();
        }
        //return fragment;
    }

    @Override
    public void onSteelSelected(int steel) {
        Log.i("MainActivity", "onSteelSelected");
        _steelDetails.view(steel);
        _activateFragment(SteelFragment.class.getName(), true);
    }

    @Override
    public void onPropertySearch(List data) {
        SteelPropertyList.Prop filter = new SteelPropertyList.Prop("");
        for (Object i : data) {
            if (i instanceof SearchFragment.Composition)
                filter.elements.add(((SearchFragment.Composition)i).toElement());
            else if (i instanceof  SearchProperty)
                filter.properties.add((SearchProperty)i);
        }
        _propertySearch.search(filter);
        _activateFragment(ResultFragment.class.getName(), true);
    }

//
//    @Override
//    public void register(IDataProvider<SQLiteHelper.SteelList> provider) {
//        _steelListProvider = provider;
//        Log.i("Register-DataProvider", provider.toString());
//    }
//
//    @Override
//    public void register(IDataRequester<SQLiteHelper.SteelList> requester) {
//        _steelListRequester = requester;
//        Log.i("Register-DataRequester", requester.toString());
//    }
//
//    @Override
//    public void unregister(IDataProvider<SQLiteHelper.SteelList> provider) {
//        if (_steelListProvider == provider)
//            _steelListProvider = null;
//    }
//
//    @Override
//    public void unregister(IDataRequester<SQLiteHelper.SteelList> requester) {
//        if (_steelListRequester == requester)
//            _steelListRequester = null;
//    }
//
//    @Override
//    public IDataProvider<SQLiteHelper.SteelList> provider() {
//        return _steelListProvider;
//    }
//
//    @Override
//    public IDataRequester<SQLiteHelper.SteelList> requester() {
//        return _steelListRequester;
//    }
//
//    @Override
//    public IDataNegotiator<SQLiteHelper.SteelList> negotiator() { return this; }
//

//    public SteelFragment viewSteel(int steel) {
//        //Bundle bundle = new Bundle();
//        //bundle.putInt("STEEL", steel);
//        SteelFragment fragment = (SteelFragment) _activateFragment(SteelFragment.class.getName(), true);
//        //fragment.setActive(steel);
//        return fragment;
//    }
//
//    public ResultFragment searchComposition(List<SQLiteHelper.Element> l) {
//        if (!l.isEmpty()) {
//            //Bundle bundle = new Bundle();
//            ResultFragment fragment = (ResultFragment) _activateFragment(ResultFragment.class.getName(), true);
//            //fragment.search(l, getDB());
//            return fragment;
//        }
//        return null;
//    }



//    private class FetchCompositionTask extends FetchTask<Integer> {
//        public FetchCompositionTask(DataHandlerInterface handler, int id) {
//            super(handler, id);
//        }
//
//        protected Bundle doInBackground(Integer... params) {
//            int steel = params[0];
//            SQLiteHelper db = getDB();
//            List<SQLiteHelper.Element> props = db.fetchComposition(steel);
//            Bundle bundle = new Bundle();
//            bundle.putInt("Steel", steel);
//            bundle.putParcelableArrayList("Composition", props);
//            return bundle;
//        }
//    }
//
//    private class FetchMechanicalPropsTask extends FetchTask<Integer> {
//        int steel;
//
//        public FetchMechanicalPropsTask(DataHandlerInterface handler, int id) {
//            super(handler, id);
//        }
//
//        protected Bundle doInBackground(Integer... params) {
//            steel = params[0];
//            SQLiteHelper db = getDB();
//            List<SQLiteHelper.MechanicalProps> props = db.fetchMechanicalProps(steel);
//            Bundle bundle = new Bundle();
//            bundle.putInt("Steel", steel);
//            bundle.putParcelableArrayList("MechanicalProps", props);
//            return bundle;
//        }
//    }
//
//    private class FetchPhysicalPropsTask extends FetchTask<Integer> {
//        int steel;
//
//        public FetchPhysicalPropsTask(DataHandlerInterface handler, int id) {
//            super(handler, id);
//        }
//
//        protected Bundle doInBackground(Integer... params) {
//            steel = params[0];
//            SQLiteHelper db = getDB();
//            List<SQLiteHelper.PhysicalProps> props = db.fetchPhysicalProps(steel);
//            Bundle bundle = new Bundle();
//            bundle.putInt("Steel", steel);
//            bundle.putParcelableArrayList("PhysicalProps", props);
//            return bundle;
//        }
//    }
//
//    private class FetchHeatTreatTask extends FetchTask<Integer> {
//        int steel;
//
//        public FetchHeatTreatTask(DataHandlerInterface handler, int id) {
//            super(handler, id);
//        }
//
//        protected Bundle doInBackground(Integer... params) {
//            steel = params[0];
//            SQLiteHelper db = getDB();
//            List<SQLiteHelper.HeatTreat> props = db.fetchHeatTreat(steel);
//            Bundle bundle = new Bundle();
//            bundle.putInt("Steel", steel);
//            bundle.putParcelableArrayList("HeatTreatment", props);
//            return bundle;
//        }
//    }

}

