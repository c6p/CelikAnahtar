package com.gulaghad.test.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements CompositionDialogFragment.SuperListener {

    private MainActivity _context;
    private CompositionAdapter _adapter;
    //public CompositionAdapter getAdapter() { return _adapter; }

    public static class Composition {
        public String name;
        public float min=-1, max=-1, value=-1, tolerance=0;
        public Composition() { name = ""; }
        public Composition(String pname) { name = pname; }
    }

    class CompositionAdapter extends BaseAdapter {

        Context context;
        ArrayList<Composition> data;
        List<SQLiteHelper.Element> elements = new ArrayList<SQLiteHelper.Element>();
        private LayoutInflater inflater = null;

        public CompositionAdapter(Context pcontext, ArrayList<Composition> pdata) {
            context = pcontext;
            data = pdata;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(Composition c) {
            // search element
            int i = 0, size = data.size();
            for (; i<size; i++) {
                if (data.get(i).name.equals(c.name))
                    break;
            }
            Log.i(Integer.toString(i), Integer.toString(size) + c.name);
            if (i < size)   // found
                data.set(i, c);
            else            // not found
                data.add(c);
            notifyDataSetChanged();
        }
        public void removeItem(int pos) {
            data.remove(pos);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }
        @Override
        public Composition getItem(int position) {
            return data.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = inflater.inflate(R.layout.search_item, null);
            TextView text = (TextView) vi.findViewById(R.id.compo_text);
            text.setTag(pos);
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addItem(data.get((Integer) v.getTag()));
                }
            });
            float tolerance = data.get(pos).value < 0
                    ? 0
                    : data.get(pos).value * data.get(pos).tolerance / 100;
            float min = data.get(pos).min < 0
                    ? 0
                    : data.get(pos).min;
            min = data.get(pos).value < 0
                    ? min
                    : Math.max(data.get(pos).value - tolerance, min);
            float max = data.get(pos).max < 0
                    ? 100
                    : Math.min(data.get(pos).max, 100);
            max = data.get(pos).value < 0
                    ? max
                    : Math.min(data.get(pos).value + tolerance, max);
            elements.add(new SQLiteHelper.Element(data.get(pos).name, min, max));
            String span = String.format("<b>%s</b> <font color='#aaaaaa'>min: </font>%.2f%%,"
                    + " <font color='#aaaaaa'>max: </font>%.2f%%",
                    data.get(pos).name, min, max);
            text.setText(Html.fromHtml(span));
            Button button = (Button) vi.findViewById(R.id.compo_remove);
            button.setTag(pos);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { removeItem((Integer) v.getTag()); }
            });
            return vi;
        }
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        return fragment;
    }

    public SearchFragment() {
    }

    @Override
    public void onOK(Composition c) {
        if (c != null)
            _adapter.addItem(c);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (_context == null)
            _context = (MainActivity) getActivity();
        _adapter = new CompositionAdapter(_context, new ArrayList<Composition>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        _context = (MainActivity) getActivity();
        ListView listview = (ListView) view.findViewById(R.id.listView);
        listview.setAdapter(_adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showComposition(_adapter.getItem(position));
            }
        });
        Button buttonAdd = (Button) view.findViewById(R.id.compo_add);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComposition(new Composition());
            }
        });
        Button buttonSearch = (Button) view.findViewById(R.id.compo_search);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity ma = (MainActivity) getActivity();
                ma.searchComposition(_adapter.elements);
            }
        });
        return view;
    }

    void showComposition(Composition c) {
        Bundle bundle = new Bundle();
        bundle.putString("NAME", c.name);
        bundle.putFloat("MIN", c.min);
        bundle.putFloat("MAX", c.max);
        bundle.putFloat("VALUE", c.value);
        bundle.putFloat("TOLERANCE", c.tolerance);
        DialogFragment d = CompositionDialogFragment.newInstance(this);
        d.setArguments(bundle);
        d.show(getFragmentManager(), "composition");
    }
}
