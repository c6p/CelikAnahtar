package com.gulaghad.test.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.Arrays;
import java.util.List;

import static android.widget.SearchView.OnQueryTextListener;
import static com.gulaghad.test.app.SQLiteHelper.PropertyList;
import static com.gulaghad.test.app.SQLiteHelper.SearchProperty;
import static com.gulaghad.test.app.SQLiteHelper.SteelPropertyList;
import static com.gulaghad.test.app.SQLiteHelper.StandardSteelList;

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
        Log.i(this.toString(), filter);
        if (!filter.isEmpty()) {
            SQLiteHelper.SteelList results = db().fetchSteel(_fetchPosition, FETCH_LENGTH, filter);
            Log.i(this.toString(), results.toString());
            return results;
        }
        return null;
    }
}

class FetchStandardTask extends FetchTask<String, SQLiteHelper.StandardList> {
    private final int FETCH_LENGTH = 32;
    private int _fetchPosition;
    public FetchStandardTask(SQLiteHelper db, IDataHandler handler, int position) {
        super(db, handler);
        _fetchPosition = position;
    }

    @Override
    protected SQLiteHelper.StandardList doInBackground(String... params) {
        String filter = params[0];
        Log.i(this.toString(), filter);
        if (!filter.isEmpty()) {
            SQLiteHelper.StandardList results = db().fetchStandard(_fetchPosition, FETCH_LENGTH, filter);
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

class FetchStandardSteelTask extends FetchTask<Integer, Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList>> {
    public FetchStandardSteelTask(SQLiteHelper db, IDataHandler handler) {
        super(db, handler);
    }

    @Override
    protected Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList> doInBackground(Integer... params) {
        Integer id = params[0];
        SQLiteHelper.Standard standard = db().fetchStandard(id);
        SQLiteHelper.StandardSteelList results = db().fetchStandardSteel(id);
        Log.i(this.toString(), results.toString());
        return Pair.create(standard, results);
    }
}


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
    private SQLiteHelper _db;
    private FetchSteelTask _task = null;

    SteelSearch(SQLiteHelper db, MainActivity ma)
    {
        assert db != null;
        this._db = db;
        Register.steelListDataProvider.set(this);
    }

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
        private FetchPropertyTask[] _tasks = new FetchPropertyTask[PropertyType.values().length];

        public Handler() { }

        private boolean isPropertyCacheNull(PropertyList cache, PropertyType type) {
            if (cache != null) {
                switch (type) {
                    case Info:
                        return cache.info == null;
                    case Composition:
                        return cache.composition == null;
                    case Mechanical:
                        return cache.mechanicalProps == null;
                    case Physical:
                        return cache.physicalProps == null;
                    case Heat:
                        return cache.heatTreats == null;
                    case Norm:
                        return cache.standards == null;
                }
            }
            return true;
        }

        public void fetch(int steel) {
            if (Register.steelId == steel)
                return;
            Register.steelId = steel;

            for (PropertyType sp : PropertyType.values()) {
                int i = sp.ordinal();
                if (_tasks[i] != null) {
                    _tasks[i].cancel(true);
                    _tasks[i] = null;
                }
            }

            PropertyList cache = null;
            if (Cache.propertyList.containsKey(Register.steelId))
                cache = Cache.propertyList.get(Register.steelId);
            for (PropertyType sp : PropertyType.values()) {
                int i = sp.ordinal();
                if (isPropertyCacheNull(cache, sp)) {
                    _tasks[i] = new FetchPropertyTask(_db, this, sp);
                    _tasks[i].execute(Register.steelId);
                }
            }
        }

        @Override
        public void setData(Pair<PropertyType, List> data) {
            Log.i("Handler", "setData");
            if (data == null)
                return;

            PropertyList cache;
            if (Cache.propertyList.containsKey(Register.steelId))
                cache = Cache.propertyList.get(Register.steelId);
            else {
                cache = new PropertyList(Register.steelId);
                Cache.propertyList.put(Register.steelId, cache);
            }

            switch (data.first) {
                case Info:
                    cache.info = data.second;
                    break;
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
                case Norm:
                    cache.standards = data.second;
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
        if (Cache.propertyList.containsKey(Register.steelId)) {
            return Cache.propertyList.get(Register.steelId);
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

class StandardSearch implements IDataProvider<SQLiteHelper.StandardList> {
    private SQLiteHelper _db;
    private FetchStandardTask _task = null;

    StandardSearch(SQLiteHelper db, MainActivity ma)
    {
        assert db != null;
        this._db = db;
        Register.standardListDataProvider.set(this);
    }

    private void _fetch(int fetchPosition) {
        if (!Register.standardFilter.isEmpty() && _task == null)
            _task = (FetchStandardTask) new FetchStandardTask(_db, this, fetchPosition).execute(Register.standardFilter);
    }
    private void _cancel() {
        if (_task != null) {
            _task.cancel(true);
            _task = null;
        }
    }
    public void search(String s) {
        Log.i("searchView2", s);
        if (Register.standardFilter.equals(s)) {
            return;
        }
        _cancel();
        Register.standardFilter = s;
        requestData(0);
    }

    @Override
    public void requestData(int size) {
        if (Cache.standardList.containsKey(Register.standardFilter)) {
            SQLiteHelper.StandardList data = Cache.standardList.get(Register.standardFilter);
            Log.i("requestData2", "DATA2");
            if (!data.complete && size >= data.position) {
                Log.i("requestData2", Integer.toString(data.position));
                _fetch(data.position);
            }
            IDataRequester<SQLiteHelper.StandardList> requester = Register.standardListDataRequester.get();
            if (requester != null)
                requester.dataReady();
        } else {
            _fetch(0);
        }
    }

    @Override
    public SQLiteHelper.StandardList getData() {
        if (Cache.standardList.containsKey(Register.standardFilter)) {
            return Cache.standardList.get(Register.standardFilter);
        }
        return null;
    }

    @Override
    public void setData(SQLiteHelper.StandardList data) {
        if (data == null)
            return;
        if (Cache.standardList.containsKey(Register.standardFilter)) {
            // merge data
            SQLiteHelper.StandardList cache = Cache.standardList.get(Register.standardFilter);
            if (cache.position < data.position) {
                cache.standards.addAll(data.standards);
                cache.complete = data.complete;
                cache.position = data.position;
            }
        } else
            Cache.standardList.put(Register.standardFilter, data);

        IDataRequester<SQLiteHelper.StandardList> requester = Register.standardListDataRequester.get();
        if (requester != null)
            requester.dataReady();

        _task = null;
    }
}

class StandardDetails implements IDataProvider<Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList>> {
    private SQLiteHelper _db;
    private Handler _handler = new Handler();

    public StandardDetails(SQLiteHelper db) {
        assert db != null;
        _db = db;
        Register.standardSteelListDataProvider.set(this);
    }

    class Handler implements IDataHandler<Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList>> {
        FetchStandardSteelTask _task = null;
        @Override
        public void setData(Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList> data) {
            if (data == null)
                return;
            Cache.standardSteelList.put(Register.standardId, data);
            Log.i("STANDARD", "Put Cache" + data.second.steelNames.size());
            StandardDetails.this.setData(data);
        }

        public void fetch(Integer standard) {
            if (Register.standardId == standard)
                return;
            Register.standardId = standard;
            if (!Cache.standardSteelList.containsKey(standard)) {
                _task = new FetchStandardSteelTask(_db, this);
                _task.execute(standard);
            }
        }
    }

    public void view(Integer standard) {
        Log.i("StandardDetails", "view");
        _handler.fetch(standard);
    }

    @Override
    public void requestData(int size) { }

    @Override
    public Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList> getData() {
        if (Cache.standardSteelList.containsKey(Register.standardId)) {
            Log.i("STANDARD", "Get Cache" + Cache.standardSteelList.get(Register.standardId).second.steelNames.size());
            return Cache.standardSteelList.get(Register.standardId);
        }
        return null;
    }

    @Override
    public void setData(Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList> data) {
        IDataRequester<Pair<SQLiteHelper.Standard, SQLiteHelper.StandardSteelList>> requester = Register.standardSteelListDataRequester.get();
        if (requester != null)
            requester.dataReady();
    }
}

interface OnStandardSelectedListener {
    public void onStandardSelected(Integer standard);
}
interface OnSteelSelectedListener {
    public void onSteelSelected(int steel);
}
interface OnPropertySearchListener {
    public void onPropertySearch(List data);
}

public class MainActivity extends Activity implements OnSteelSelectedListener, OnPropertySearchListener, OnStandardSelectedListener {
    private SQLiteHelper _db;
    private SteelSearch _steelSearch;
    private SteelDetails _steelDetails;
    private PropertySearch _propertySearch;
    private StandardSearch _standardSearch;
    private StandardDetails _standardDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeButtonEnabled(true);

        _db = SQLiteHelper.get(getApplicationContext());
        _steelSearch = new SteelSearch(_db, this);
        _steelDetails = new SteelDetails(_db);
        _propertySearch = new PropertySearch(_db);
        _standardSearch = new StandardSearch(_db, this);
        _standardDetails = new StandardDetails(_db);

        if (savedInstanceState == null) {
            _activateFragment(SearchFragment.class.getName(), false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        final MenuItem searchItem2 = menu.findItem(R.id.action_search2);
        final SearchView searchView2 = (SearchView) searchItem2.getActionView();
//        final MenuItem homeItem = menu.findItem(android.R.id.home);

//        homeItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                _activateFragment(SearchFragment.class.getName(), true);
//                return false;
//            }
//        });
        searchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                searchItem2.collapseActionView();
                return false;
            }
        });
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return search(query);
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return search(newText);
            }
            private boolean search(String s) {
                if (Register.queryFocused)
                    _steelSearch.search(s);
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

        // hack search image
        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView v = (ImageView) searchView2.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_menu_find_holo_dark);

        searchItem2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                searchItem.collapseActionView();
                return false;
            }
        });
        searchView2.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return search(query);
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return search(newText);
            }
            private boolean search(String s) {
                if (Register.query2Focused)
                    _standardSearch.search(s);
                return true;
            }
        });
        searchView2.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean queryTextFocused) {
                Register.query2Focused = queryTextFocused;
                if(!queryTextFocused) {
                    searchItem2.collapseActionView();
                } else {
                    searchView2.setQuery(Register.standardFilter, true);
                    _activateFragment(StandardSearchFragment.class.getName(), true);
                }
            }
        });
        searchView2.setQueryHint(getResources().getString(R.string.hint_search2));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                _activateFragment(SearchFragment.class.getName(), true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void _activateFragment(String tag, boolean backstack) {
        FragmentManager fm = getFragmentManager();
        fm.popBackStackImmediate(tag, 0);

        if (fm.getBackStackEntryCount() < 1 || !fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1).getName().equals(tag)) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment == null)
                fragment = Fragment.instantiate(MainActivity.this, tag);
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(android.R.id.content, fragment, tag);
            if (backstack)
                fragmentTransaction.addToBackStack(tag);
            fragmentTransaction.commit();
        }
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

    @Override
    public void onStandardSelected(Integer standard) {
        Log.i("MainActivity", "onStandardSelected");
        _standardDetails.view(standard);
        _activateFragment(StandardFragment.class.getName(), true);
    }
}

