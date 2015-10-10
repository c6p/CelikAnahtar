package com.gulaghad.test.app;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SearchFragment extends Fragment implements CompositionDialogFragment.SuperListener, PropertyDialogFragment.SuperListener {

    private MainActivity _context;
    private CompositionAdapter _adapter;

    //public CompositionAdapter getAdapter() { return _adapter; }

    //format
    private static String f(float f)
    {
        if(f == (int) f)
            return String.format("%d",(int)f);
        else
            return String.format("%s",f);
    }

    public static class Composition {
        public String name;
        public float min=-1f, max=-1f, value=-1f, tolerance=0f;
        public Composition() { name = ""; }
        public Composition(String pname) { name = pname; }
        public SQLiteHelper.Element toElement() {
            float tolerance = this.value < 0
                    ? 0
                    : this.value * this.tolerance / 100;
            float min = this.min < 0
                    ? 0
                    : this.min;
            min = this.value < 0
                    ? min
                    : Math.max(this.value - tolerance, min);
            float max = this.max < 0
                    ? 100
                    : Math.min(this.max, 100);
            max = this.value < 0
                    ? max
                    : Math.min(this.value + tolerance, max);
            return new SQLiteHelper.Element(name, min, max);
        }
        public String name() { return name; }
        public String min() { return min == -1f ? "" : f(min); }
        public String max() { return max == -1f ? "" : f(max); }
        public String value() { return value == -1f ? "" : f(value); }
        public String tolerance() { return tolerance == 0f ? "" : f(tolerance); }
    }

    class CompositionAdapter extends BaseAdapter {

        Context context;
//        private List<Object> data = new ArrayList<Object>();

//        private List<SQLiteHelper.Element> elements = new ArrayList<SQLiteHelper.Element>();
//        private List<SQLiteHelper.SearchProperty> properties = new ArrayList<SQLiteHelper.SearchProperty>();
        private LayoutInflater inflater = null;

        public CompositionAdapter(Context pcontext) {
            context = pcontext;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(Object item) {
            // search element
            List<Object> data = Register.propertySearch;
            int i = 0, size = data.size();
            for (; i<size; i++) {
                if ( item instanceof Composition &&
                        data.get(i) instanceof Composition &&
                        ((Composition)data.get(i)).name.equals(
                                ((Composition)item).name) )
                    break;
                else if ( item instanceof SQLiteHelper.SearchProperty &&
                        data.get(i) instanceof SQLiteHelper.SearchProperty &&
                        ((SQLiteHelper.SearchProperty)data.get(i)).property.equals(
                                ((SQLiteHelper.SearchProperty)item).property) )
                    break;
            }
//            Log.i(Integer.toString(i), Integer.toString(size) + c.name);
            if (i < size)   // found
                data.set(i, item);
            else            // not found
                data.add(item);
            notifyDataSetChanged();
        }

        public void removeItem(int pos) {
            Register.propertySearch.remove(pos);
            notifyDataSetChanged();
        }

        // join
        private String j(Float min, Float max) {
            if (min == null)
                min = 0f;
            if (max == null)
                max = 0f;
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

        @Override
        public int getCount() {
            return Register.propertySearch.size();
        }
        @Override
        public Object getItem(int position) {
            return Register.propertySearch.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        private Pair<String, String> span(Composition c) {
            SQLiteHelper.Element e = c.toElement();
//            String span = String.format("%s: <font color='#cccccc'>%s</font>",
//                    e.name, j(e.min, e.max));
            Pair<String, String> span = new Pair<String, String>(e.name, j(e.min, e.max)+" %");
            return span;
        }

        private Pair<String, String> span(SQLiteHelper.SearchProperty sp) {
//            String span = String.format("%s: <font color='#cccccc'>%s</font>",
//                    sp.property, j(sp.value_min, sp.value_max));
            LinkedHashMap<String, Pair<Integer, String>> properties =
                    SQLiteHelper.get(getActivity().getApplicationContext()).properties();
            String unit = properties.get(sp.property).second;
            Pair<String, String> span = new Pair<String, String>(sp.property, j(sp.value_min, sp.value_max) + " " + unit);
            return span;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = inflater.inflate(R.layout.search_item, null);
            TextView text = (TextView) vi.findViewById(R.id.compo_text);
            TextView value = (TextView) vi.findViewById(R.id.compo_value);

            final List<Object> data = Register.propertySearch;

            vi.setTag(pos);
            vi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Object item = data.get((Integer) v.getTag());
                    if (item instanceof Composition)
                        showComposition((Composition) item);
                    else
                        showProperty((SQLiteHelper.SearchProperty)item);
                }
            });

            Object o = data.get(pos);
            Pair<String, String> s;
            if (o instanceof Composition)
                s = span((Composition) o);
            else // o instanceof SearchProperty
                s = span((SQLiteHelper.SearchProperty) o);
            text.setText(s.first);
            value.setText(s.second);

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
    public void onOK(SQLiteHelper.SearchProperty p) {
        if (p != null)
            _adapter.addItem(p);
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
        _adapter = new CompositionAdapter(_context);
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
//                showComposition(_adapter.getItem(position));
            }
        });

        Button buttonAddCompo = (Button) view.findViewById(R.id.compo_add);
        buttonAddCompo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComposition(null);
            }
        });

        Button buttonAddProp = (Button) view.findViewById(R.id.property_add);
        buttonAddProp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProperty(null);
            }
        });

        Button buttonSearch = (Button) view.findViewById(R.id.property_search);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnPropertySearchListener listener = (OnPropertySearchListener) getActivity();
                listener.onPropertySearch(Register.propertySearch);
            }
        });
        return view;
    }

    void showProperty(SQLiteHelper.SearchProperty i) {
        Bundle bundle = new Bundle();
        if (i != null) {
            Log.i("", i.toString());
            bundle.putString("PROPERTY", i.property());
            bundle.putString("STATE", i.state());
            bundle.putString("VALUE_MIN", i.value_min());
            bundle.putString("VALUE_MAX", i.value_max());
            bundle.putString("DIM_MIN", i.dim_min());
            bundle.putString("DIM_MAX", i.dim_max());
            bundle.putString("TEMP_MIN", i.temp_min());
            bundle.putString("TEMP_MAX", i.temp_max());
        }
        DialogFragment d = PropertyDialogFragment.newInstance(this);
        d.setArguments(bundle);
        d.show(getFragmentManager(), "property");
    }

    void showComposition(Composition c) {
        Bundle bundle = new Bundle();
        if (c != null) {
            Log.i("", c.toString());
            bundle.putString("NAME", c.name());
            bundle.putString("MIN", c.min());
            bundle.putString("MAX", c.max());
            bundle.putString("VALUE", c.value());
            bundle.putString("TOLERANCE", c.tolerance());
        }
        DialogFragment d = CompositionDialogFragment.newInstance(this);
        d.setArguments(bundle);
        d.show(getFragmentManager(), "composition");
    }
}
