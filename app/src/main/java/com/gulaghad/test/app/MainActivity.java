package com.gulaghad.test.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import static android.support.v7.widget.SearchView.OnQueryTextListener;


public class MainActivity extends ActionBarActivity {

    private int _navigationIndex=-1;
    private static final int LIST_STEELS = 0, VIEW_STEEL = 1;
    private SQLiteHelper _db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Notice that setContentView() is not used, because we use the root
        // android.R.id.content as the container for each fragment

        // setup action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);

        ArrayAdapter spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.action_list, android.R.layout.simple_spinner_dropdown_item);

        ActionBar.OnNavigationListener onNavigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long itemId) {
                if (_navigationIndex != position) {
                    _navigationIndex = position;
                    String type = "";
                    switch (position) {
                        case LIST_STEELS:
                            type = RecentFragment.class.getName();
                            break;
                        //case 1:
                        //    type = SearchFragment.class.getName();
                         //   break;
                        case VIEW_STEEL:
                            type = SteelFragment.class.getName();
                            break;
                    }
                    _activateFragment(type, new Bundle());
                    return true;
                }
                return false;
            }
        };

        actionBar.setListNavigationCallbacks(spinnerAdapter, onNavigationListener);

        //ActionBar.Tab tab = actionBar.newTab()
        //        .setText(R.string.recent)
        //        .setTabListener(new TabListener<RecentFragment>(
        //                this, "recent", RecentFragment.class));
        //actionBar.addTab(tab);
        //
        //tab = actionBar.newTab()
        //        .setText(R.string.d)
        //        .setTabListener(new TabListener<SearchFragment>(
        //                this, "search", SearchFragment.class));
        //actionBar.addTab(tab);
        //
        //tab = actionBar.newTab()
        //        .setText(R.string.results)
        //        .setTabListener(new TabListener<ResultsFragment>(
        //                this, "results", ResultsFragment.class));
        //actionBar.addTab(tab);
        //
        //tab = actionBar.newTab()
        //        .setText(R.string.steel)
        //        .setTabListener(new TabListener<SteelFragment>(
        //                this, "steel", SteelFragment.class));
        //actionBar.addTab(tab);
        _db = new SQLiteHelper(this);
    }
    public SQLiteHelper getDB() {
        return _db;
    }

    private Fragment _activateFragment(String tag, Bundle bundle) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if (! (fragment != null && fragment.isVisible()) ) {
            if (fragment == null)
                fragment = Fragment.instantiate(MainActivity.this, tag);
            fragment.setArguments(bundle);
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(android.R.id.content, fragment, tag);
            fragmentTransaction.commit();
        }
        return fragment;
    }

    public SteelFragment viewSteel(int steel) {
        Bundle bundle = new Bundle();
        bundle.putInt("STEEL", steel);
        SteelFragment fragment = (SteelFragment) _activateFragment(SteelFragment.class.getName(), bundle);
        _navigationIndex = VIEW_STEEL;
        getSupportActionBar().setSelectedNavigationItem(VIEW_STEEL);
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            private String _s = "";
            @Override
            public boolean onQueryTextSubmit(String s) {
                MenuItemCompat.collapseActionView(searchItem);
                return _filter(s);
            }
            @Override
            public boolean onQueryTextChange(String s) {
                return _filter(s);
            }
            private boolean _filter(String s) {
                if (!_s.equals(s)) {
                    _s = s;
                    Bundle bundle = new Bundle();
                    // TODO bundle search filter
                    RecentFragment fragment = (RecentFragment) _activateFragment(RecentFragment.class.getName(), bundle);
                    fragment.search(s);
                    _navigationIndex = LIST_STEELS;
                    getSupportActionBar().setSelectedNavigationItem(LIST_STEELS);
                    return true;
                }
                return false;
            }
        });
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
}
