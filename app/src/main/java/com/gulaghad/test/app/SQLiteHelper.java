package com.gulaghad.test.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

enum PropertyType { Info, Composition, Mechanical, Physical, Heat, Standard, }

public class SQLiteHelper extends SQLiteAssetHelper {
    private static SQLiteHelper _instance = null;

    private static final String DATABASE_NAME = "celik.db";
    private static final int DATABASE_VERSION = 2;
    private static final String LOG = "SQLiteHelper";

    // id, (code, country)
    private final SparseArray<Pair<String, String>> _countries;
    private final SparseArray<String> _normtypes;
    private final LinkedHashMap<String, Pair<Integer, String>> _properties;      // property, pair<id, unit>
    private final HashMap<String, Integer> _states;
    private final SQLiteDatabase _db;

    private SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.setForcedUpgrade();
        _db = this.getReadableDatabase();
        _countries = _fetchCountries();
        _normtypes = _fetchNormTypes();
        _properties = _fetchProperties();
        _states = _setStates();
    }

    public static SQLiteHelper get(Context context) {
        if (_instance == null)
            _instance = new SQLiteHelper(context);
        return _instance;
    }

    private SparseArray<Pair<String, String>> _fetchCountries() {
        SparseArray<Pair<String, String>> countries = new SparseArray<Pair<String, String>>();
        String q = "SELECT id_country, code, country FROM country";
        Log.e(LOG, q);
        Cursor c = _db.rawQuery(q, null);
        if (c.moveToFirst()) {
            do {
                countries.put(c.getInt(0),
                        Pair.create(c.getString(1), c.isNull(2) ? "" : c.getString(2)));
            } while (c.moveToNext());
        }
        return countries;
    }

    private SparseArray<String> _fetchNormTypes() {
        SparseArray<String> normtypes = new SparseArray<String>();
        String q = "SELECT id_norm, normtype FROM normtype";
        Log.e(LOG, q);
        Cursor c = _db.rawQuery(q, null);
        if (c.moveToFirst()) {
            do {
                normtypes.put(c.getInt(0), c.getString(1));
            } while (c.moveToNext());
        }
        return normtypes;
    }

    private LinkedHashMap<String, Pair<Integer, String>> _fetchProperties() {
        LinkedHashMap<String, Pair<Integer, String>> properties = new LinkedHashMap<String, Pair<Integer, String>>();
        String q = "SELECT id_property, property, unit FROM property WHERE b_search=1";
        Log.e(LOG, q);
        Cursor c = _db.rawQuery(q, null);
        if (c.moveToFirst()) {
            do {
                properties.put(c.getString(1),
                        Pair.create(c.getInt(0), c.getString(2)));
            } while (c.moveToNext());
        }
        return properties;
    }


    private HashMap<String, Integer> _setStates() {
        HashMap<String, Integer> states = new HashMap<String, Integer>();
        states.put("", null);
        states.put("untreated", 40);
        states.put("annealed", 4);
        states.put("hardened/tempered/quenched", 41);
        states.put("tempered", 2);
        states.put("case hardened", 33);
        states.put("cold formed", 9);
        return states;
    }

    public LinkedHashMap<String, Pair<Integer, String>> properties() {
        return _properties;
    }

    public static class SteelList {
        public final String filter;
        public boolean complete = false;
        public int position = 0;
        public ArrayList<Integer> steelIds = new ArrayList<Integer>();
        public ArrayList<String> steelNames = new ArrayList<String>();

        SteelList(String filter) {
            this.filter = filter;
        }
    }

    public SteelList fetchSteel(int pos, int size, String filter) {
        String q = "SELECT name, id_country, sn.id_steel FROM steelname AS sn LEFT JOIN steel AS s ON sn.id_steel = s.id_steel";
        Cursor c;
        if (TextUtils.isEmpty(filter)) {
            q += " AND namestatus = ?";
            c = _db.rawQuery(q, new String[]{"1"});
        } else {
            q += " WHERE namestatus = ?";
            q += " AND name LIKE ?";
            c = _db.rawQuery(q, new String[]{"1", "%"+filter+"%", });
        }
        Log.i(LOG, q);
        int counter = 0;
        SteelList list = new SteelList(filter);
        if (c.moveToPosition(pos)) {
            do {
                list.steelNames.add(c.getString(0) + " (" + _countries.get(c.getInt(1)).first + ")");
                list.steelIds.add(c.getInt(2));
            } while (c.moveToNext() && ++counter < size);
            list.complete = counter != size;
            list.position = c.getPosition();
        }
        c.close();
        return list;
    }

    public static class SteelPropertyList {
        public static class Prop {
            public String name;
            public List<Element> elements = new ArrayList<Element>();
            public List<SearchProperty> properties = new ArrayList<SearchProperty>();
            public Prop(String n) { name = n; }
        }
        public Prop filter;
        public boolean complete = false;
        public int position = 0;
        public ArrayList<Integer> steelIds = new ArrayList<Integer>();
        public ArrayList<String> steelNames = new ArrayList<String>();
        public ArrayList<Prop> steelProps = new ArrayList<Prop>();

        public SteelPropertyList(Prop filter) {
            this.filter = filter;
        }
    }

    public static class SearchProperty {
        public String property;
        public String state;
        public Float value_min;
        public Float value_max;
        public Float dim_min;
        public Float dim_max;
        public Float temp_min;
        public Float temp_max;
        public SearchProperty(String p, String s, Float vmin, Float vmax, Float dmin, Float dmax, Float tmin, Float tmax) {
            property = p;
            state = s;
            value_min = vmin;
            value_max = vmax;
            dim_min = dmin;
            dim_max = dmax;
            temp_min = tmin;
            temp_max = tmax;
        }
    }

    private String elementParams(List<Element> elements, ArrayList<String> paramList) {
        String q = " ";
        String joiner = "";
        for (Element e : elements) {
            q += joiner;
            q += String.format(" %s_min <= ? AND %s_max >= ?",
                    e.name, e.name);
            joiner = " AND";
            paramList.add(Float.toString(e.max));
            paramList.add(Float.toString(e.min));
        }
        // Only desired elements
//        q += " AND (";
//        joiner = "";
//        for (Element e : elements) {
//            q += joiner + " an.id_element = ?";
//            joiner = " OR";
//            paramList.add(e.name);
//        }
//        q += ")";
        return q;
    }
    private String propertyParams(List<SearchProperty> properties, ArrayList<String> paramList) {
        String q = "(";
        String joiner = "";
        for (SearchProperty p : properties) {
            q += joiner;
            q += "(pv.id_property = ?";
            paramList.add(_properties.get(p.property).first.toString());
            if (!p.state.isEmpty()) {
                q += " AND pv.sid_state = ?";
                paramList.add(_states.get(p.state).toString());
            }
            q += ")";
            joiner = " OR";
        }
        q += ")";

        q += " AND spv.id_steel IN (";
        joiner = "";
        for (SearchProperty p : properties) {
            q += joiner;
            q += " SELECT id_steel FROM propertyvalue as pv"
                    + " LEFT JOIN steel_propertyvalue AS spv ON pv.id_propertyvaluematrix == spv.id_propertyvalue"
                    + " WHERE pv.id_property = ?";
            paramList.add(_properties.get(p.property).first.toString());
            if (!p.state.isEmpty()) {
                q += " AND pv.sid_state = ?";
                paramList.add(_states.get(p.state).toString());
            }
            if (p.value_min != null) {
                q += " AND value_min >= ?";
                paramList.add(p.value_min.toString());
            }
            if (p.value_max != null) {
                q += " AND value_max <= ?";
                paramList.add(p.value_max.toString());
            }
            if (p.dim_min != null) {
                q += " AND dim_min >= ?";
                paramList.add(p.dim_min.toString());
            }
            if (p.dim_max != null) {
                q += " AND dim_max <= ?";
                paramList.add(p.dim_max.toString());
            }
            if (p.temp_min != null) {
                q += " AND temp_min >= ?";
                paramList.add(p.temp_min.toString());
            }
            if (p.temp_max != null) {
                q += " AND temp_max <= ?";
                paramList.add(p.temp_max.toString());
            }
            joiner = " INTERSECT";
        }
        q += " GROUP BY id_steel)";

        return q;
    }

    public SteelPropertyList fetchSteelProperty(int pos, int size, SteelPropertyList.Prop param) {
        SteelPropertyList list = new SteelPropertyList(param);
        size = size * ((list.filter.elements.isEmpty() ? 0 : 8) + list.filter.properties.size());
        boolean bElement = false, bProperty = false;
        int end = pos;
        String q;
        ArrayList<String> paramList = new ArrayList<String>();
        if (param.properties.isEmpty()) {
            if (param.elements.isEmpty())
                return list;

            bElement = true;
            q = "SELECT a.id_steel, sn.name, an.id_element, id_min, id_max FROM analysesearch AS s"
                    + " LEFT JOIN analyse AS a ON s.id_analyse == a.id_analyse"
                    + " LEFT JOIN steelname AS sn ON a.id_steel == sn.id_steel"
                    + " LEFT JOIN analyseproperty AS an ON a.id_analyse == an.id_analyse"
                    + " LEFT JOIN element AS e ON an.id_element == e.id_element";
            q += " WHERE" + elementParams(param.elements, paramList);
            q += " GROUP BY s.id_analyse, an.id_element ORDER BY a.id_steel, e.sort";
        } else {
            if (param.elements.isEmpty()) {
                bProperty = true;
                q = "SELECT spv.id_steel, sn.name, property, state, value_min, value_max,"
                        + " temp_min, temp_max, dimension_min, dimension_max FROM propertyvalue as pv"
                        + " LEFT JOIN steel_propertyvalue AS spv ON pv.id_propertyvaluematrix == spv.id_propertyvalue"
                        + " LEFT JOIN steelname AS sn ON spv.id_steel == sn.id_steel"
                        + " LEFT JOIN property AS p ON pv.id_property == p.id_property"
                        + " LEFT JOIN state AS s ON pv.id_state == s.id_state";
                q += " WHERE" + propertyParams(param.properties, paramList);
                q += " GROUP BY spv.id_steel, s.id_state ORDER BY spv.id_steel, s.sort, p.sort";
            } else {
                bElement = bProperty = true;
                q = "SELECT a.id_steel, sn.name, an.id_element, id_min, id_max, property, state,"
                        + " value_min, value_max, temp_min, temp_max, dimension_min, dimension_max"
                        + " FROM analysesearch AS s"
                        + " LEFT JOIN analyse AS a ON s.id_analyse == a.id_analyse"
                        + " LEFT JOIN steelname AS sn ON a.id_steel == sn.id_steel"
                        + " LEFT JOIN steel_propertyvalue AS spv ON sn.id_steel == spv.id_steel"
                        + " LEFT JOIN propertyvalue as pv ON spv.id_propertyvaluematrix == spv.id_propertyvalue"
                        + " LEFT JOIN property AS p ON pv.id_property == p.id_property"
                        + " LEFT JOIN state AS s ON pv.id_state == s.id_state"
                        + " LEFT JOIN analyseproperty AS an ON a.id_analyse == an.id_analyse"
                        + " LEFT JOIN element AS e ON an.id_element == e.id_element";
                q += " WHERE (" + elementParams(param.elements, paramList) + ") AND ";
                q += propertyParams(param.properties, paramList);
                q += " GROUP BY s.id_analyse, an.id_element ORDER BY a.id_steel, e.sort, s.sort, p.sort";
            }
        }

        Cursor c;
        c = _db.rawQuery(q, (String[]) paramList.toArray(new String[paramList.size()]));
        Log.i(LOG, q);

        int counter = 0;
        int steel = -1;
        SteelPropertyList.Prop prop = null;
        Element e = null;
        SearchProperty p = null;
        if (c.moveToPosition(pos)) {
            do {
                // steel was different
                if (steel != c.getInt(0)) {
                    if (++counter >= size)   // do not fetch anymore, if reached max-size
                        break;
                    steel = c.getInt(0);
                    String steelName = c.getString(1);

                    prop = new SteelPropertyList.Prop(steelName);
                    list.steelProps.add(prop);
                    list.steelNames.add(steelName);
                    list.steelIds.add(steel);
                }
                // read data
                e = null;
                p = null;

                if (bElement) {
                    e = new Element(c.getString(2), c.getFloat(3), c.getFloat(4));
                    if (bProperty) {
                        p = new SearchProperty(c.getString(5), c.getString(6), c.getFloat(7), c.getFloat(8),
                                c.getFloat(9), c.getFloat(10), c.getFloat(11), c.getFloat(12));
                    }
                } else if (bProperty) {
                    p = new SearchProperty(c.getString(2), c.getString(3), c.getFloat(4), c.getFloat(5),
                            c.getFloat(6), c.getFloat(7), c.getFloat(8), c.getFloat(9));
                }

                if (e != null)
                    prop.elements.add(e);
                if (p != null)
                    prop.properties.add(p);

            } while (c.moveToNext());
            // merge last steel
//            if (e != null)
//                prop.elements.add(e);
//            if (p != null)
//                prop.properties.add(p);
            list.complete = counter != size;
            list.position = c.getPosition();
        }
        c.close();
        return list;
    }

    public static class PropertyList {
//        public boolean complete;
        public int steelId;
        public List<Description> info;
        public List<Element> composition;
        public List<MechanicalProp> mechanicalProps;
        public List<PhysicalProp> physicalProps;
        public List<HeatTreat> heatTreats;
        public List<Standard> standards;

        PropertyList(int id) { steelId = id; };
    }

    public static class Description
    {
        public final String wsnr;
        public final String designation;
        public final String names;
        public final String group;
        public final String standard;
        public Description(String pwsnr, String pdesignation, String pnames, String pgroup, String pstandard) {
            wsnr = pwsnr;
            designation = pdesignation;
            names = pnames;
            group = pgroup;
            standard = pstandard;
        }
    }
    private List<Description> fetchInfo(Integer steel_id) {
        List<Description> info = new ArrayList<Description>();
        String q = "SELECT  nw.wsnrdisplay, GROUP_CONCAT(DISTINCT spv.designation), GROUP_CONCAT(DISTINCT sn.name), sg.detail, nv.standard\n" +
                " FROM steel AS s\n" +
                " LEFT JOIN steel_propertyvalue AS spv ON s.id_steel == spv.id_steel" +
                " LEFT JOIN normwsnr AS nw ON s.id_steel == nw.id_steel" +
                " LEFT JOIN steelgroup AS sg ON s.id_steelgroup == sg.id_steelgroup" +
                " LEFT JOIN steeldesignation_normvariant AS sdnv ON spv.id_steeldesignation == sdnv.id_steeldesignation" +
                " LEFT JOIN normvariant AS nv ON nv.id_normvariant == sdnv.id_normvariant" +
                " LEFT JOIN steelname AS sn ON s.id_steel == sn.id_steel" +
                " WHERE s.id_steel == ?";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            do {
                info.add(new Description(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
            } while (c.moveToNext());
        }
        c.close();
        return info;
    }

    public static class Element
    {
        public final String name;
        public final float min;
        public final float max;
        public Element(String pname, float pmin, float pmax) {
            name = pname;
            min = pmin;
            max = pmax;
        }
    }
    private List<Element> fetchComposition(Integer steel_id) {
        List<Element> elements = new ArrayList<Element>();
        String q = "SELECT an.id_element, id_min, id_max FROM analyse AS a"
                + " LEFT JOIN analyseproperty AS an ON a.id_analyse == an.id_analyse"
                + " LEFT JOIN element AS e ON an.id_element == e.id_element"
                + " WHERE id_steel == ? ORDER BY e.sort";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            do {
                elements.add(new Element(c.getString(0), c.getFloat(1), c.getFloat(2)));
            } while (c.moveToNext());
        }
        c.close();
        return elements;
    }

    public class MechanicalProp
    {
        public final String standard;
        public final String date;
        public final String title;
        public final String property;
        public final String state;
        public final int dimMin;
        public final int dimMax;
        public final float valMin;
        public final float valMax;
        public final String unit;
        public String sampleLoc;
        public final int atTempMin;
        public final int atTempMax;
        public MechanicalProp(String pstandard, String pdate, String ptitle,
                               String pproperty, String pstate, int pdimMin, int pdimMax,
                               float pvalMin, float pvalMax, String punit, String psampleLoc,
                               int patTempMin, int patTempMax) {
            standard = pstandard == null ? "" : pstandard;
            date = pdate == null ? "" : pdate;
            title = ptitle == null ? "" : ptitle;
            property = pproperty;
            state = pstate == null ? "" : pstate;
            dimMin = pdimMin;
            dimMax = pdimMax;
            valMin = pvalMin;
            valMax = pvalMax;
            unit = punit == null ? "" : punit;
            sampleLoc = psampleLoc == null ? "" : sampleLoc;
            atTempMin = patTempMin;
            atTempMax = patTempMax;
        }
    }
    private List<MechanicalProp> fetchMechanicalProps(Integer steel_id) {
        List<MechanicalProp> props = new ArrayList<MechanicalProp>();
        String q = "SELECT nv.standard, nv.date, nb.title, property, state, dimension_min, dimension_max,"
                + " value_min, value_max, unit, sampleloc, attemp_min, attemp_max"
                + " FROM steel_propertyvalue AS sp"
                + " LEFT JOIN propertyvalue AS pv ON pv.id_propertyvaluematrix == sp.id_propertyvalue"
                + " LEFT JOIN property AS p ON pv.id_property == p.id_property"
                + " LEFT JOIN state AS s ON pv.id_state == s.id_state"
                + " LEFT JOIN samplelocation AS sl ON pv.id_sampleloc == sl.id_sampleloc"
                + " LEFT JOIN normvariant AS nv ON sp.id_normvariant == nv.id_normvariant"
                + " LEFT JOIN normbase AS nb ON nv.id_normbase == nb.id_normbase"
                + " WHERE sp.id_steel == ? AND id_propertytype == 1"
                + " ORDER BY nv.id_normvariant, s.sort, p.sort, attemp_min, attemp_max;";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            do {
                props.add(new MechanicalProp(c.getString(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4),
                        c.getInt(5), c.getInt(6), c.getFloat(7), c.getFloat(8), c.getString(9),
                        c.getString(10), c.getInt(11), c.getInt(12)));
            } while (c.moveToNext());
        }
        c.close();
        return props;
    }

    public class PhysicalProp
    {
        public final String standard;
        public final String date;
        public final String title;
        public final String property;
        public final String state;
        public final String classification;
        public final float valMin, valMax;
        public final String unit;
        public final int tempMin, tempMax;
        public PhysicalProp(String pstandard, String pdate, String ptitle,
                             String pproperty, String pstate, String pclassification,
                             float pvalMin, float pvalMax, String punit,
                             int ptempMin, int ptempMax) {
            standard = pstandard == null ? "" : pstandard;
            date = pdate == null ? "" : pdate;
            title = ptitle == null ? "" : ptitle;
            property = pproperty;
            state = pstate == null ? "" : pstate;
            classification = pclassification;   // may be Null
            valMin = pvalMin;
            valMax = pvalMax;
            unit = punit == null ? "" : punit;
            tempMin = ptempMin;
            tempMax = ptempMax;
        }
    }
    private List<PhysicalProp> fetchPhysicalProps(Integer steel_id) {
        List<PhysicalProp> props = new ArrayList<PhysicalProp>();
        String q = "SELECT nv.standard, nv.date, nb.title, property, state, classification,"
                + " value_min, value_max, unit, temp_min, temp_max"
                + " FROM steel_propertyvalue AS sp"
                + " LEFT JOIN propertyvalue AS pv ON pv.id_propertyvaluematrix == sp.id_propertyvalue"
                + " LEFT JOIN property AS p ON pv.id_property == p.id_property"
                + " LEFT JOIN state AS s ON pv.id_state == s.id_state"
                + " LEFT JOIN classification AS c ON pv.id_classification == c.id_classification"
                + " LEFT JOIN normvariant AS nv ON sp.id_normvariant == nv.id_normvariant"
                + " LEFT JOIN normbase AS nb ON nv.id_normbase == nb.id_normbase"
                + " WHERE sp.id_steel == ? AND id_propertytype == 2"
                + " ORDER BY nv.id_normvariant, s.sort, p.sort, temp_min, temp_max;";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            do {
                props.add(new PhysicalProp(c.getString(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getString(5),
                        c.getFloat(6), c.getFloat(7), c.getString(8),
                        c.getInt(9), c.getInt(10)));
            } while (c.moveToNext());
        }
        c.close();
        return props;
    }

    public class HeatTreat
    {
        public final String standard;
        public final String date;
        public final String title;
        public final String property;
        public final String state;
        public final String cooling;
        public final float valMin, valMax;
        public final String unit;
        public HeatTreat(String pstandard, String pdate, String ptitle,
                         String pproperty, String pstate, String pcooling,
                         float pvalMin, float pvalMax, String punit) {
            standard = pstandard == null ? "" : pstandard;
            date = pdate == null ? "" : pdate;
            title = ptitle == null ? "" : ptitle;
            property = pproperty;
            state = pstate == null ? "" : pstate;
            cooling = pcooling == null ? "" : pcooling;
            valMin = pvalMin;
            valMax = pvalMax;
            unit = punit == null ? "" : punit;
        }
    }
    private List<HeatTreat> fetchHeatTreat(Integer steel_id) {
        List<HeatTreat> props = new ArrayList<HeatTreat>();
        String q = "SELECT nv.standard, nv.date, nb.title, property, state, cooling, value_min, value_max, unit"
                + " FROM steel_propertyvalue AS sp"
                + " LEFT JOIN propertyvalue AS pv ON pv.id_propertyvaluematrix == sp.id_propertyvalue"
                + " LEFT JOIN property AS p ON pv.id_property == p.id_property"
                + " LEFT JOIN state AS s ON pv.id_state == s.id_state"
                + " LEFT JOIN cooling AS c ON pv.id_cooling == c.id_cooling"
                + " LEFT JOIN normvariant AS nv ON sp.id_normvariant == nv.id_normvariant"
                + " LEFT JOIN normbase AS nb ON nv.id_normbase == nb.id_normbase"
                + " WHERE sp.id_steel == ? AND id_propertytype == 3"
                + " ORDER BY nv.id_normvariant, s.sort, p.sort, c.sort;";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            do {
                props.add(new HeatTreat(c.getString(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getString(5),
                        c.getFloat(6), c.getFloat(7), c.getString(8)));
            } while (c.moveToNext());
        }
        c.close();
        return props;
    }

    public static class Standard
    {
        public final String country;
        public final String name;
        public final String standard;
        public final String wsnr;
        public Standard(String pcountry, String pname, String pstandard, String pwsnr) {
            country = pcountry;
            name = pname;
            standard = pstandard;
            wsnr = pwsnr;
        }
    }
    private List<Standard> fetchStandards(Integer steel_id) {
        List<Standard> standards = new ArrayList<Standard>();
        String q = "SELECT sd.id_country, sn.name, nv.prefix, nb.name, nv.date, nv.id_norm, wsnrdisplay"
                + " FROM normwsnr AS n"
                + " JOIN steel AS s on s.id_steel = n.id_steel"
                + " JOIN steelname AS sn on s.id_steel = sn.id_steel"
                + " JOIN steeldesignation AS sd on sn.id_steelname = sd.id_steelname"
                + " JOIN steeldesignation_normvariant AS sdnv on sd.id_steeldesignation = sdnv.id_steeldesignation"
                + " JOIN normvariant AS nv on sdnv.id_normvariant = nv.id_normvariant"
                + " JOIN normbase AS nb on nv.id_normbase = nb.id_normbase"
                + " WHERE s.id_steel in (SELECT id_steel FROM normwsnr WHERE wsnr="
                + " (SELECT wsnr FROM normwsnr WHERE id_steel=?))";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            do {
                String date = c.getString(4);
                if (!date.isEmpty())
                    date = " (" + date + ")";
                standards.add(new Standard(_countries.get(c.getInt(0)).second, c.getString(1),
                        c.getString(2)+" "+c.getString(3)+date+" ("+_normtypes.get(c.getInt(5))+")",
                        c.getString(6)));
            } while (c.moveToNext());
        }
        c.close();
        return standards;
    }

    public List fetchSteelProps(Integer steel_id, PropertyType type) {
        switch (type) {
            case Info:
                return fetchInfo(steel_id);
            case Composition:
                return fetchComposition(steel_id);
            case Mechanical:
                return fetchMechanicalProps(steel_id);
            case Physical:
                return fetchPhysicalProps(steel_id);
            case Heat:
                return fetchHeatTreat(steel_id);
            case Standard:
                return fetchStandards(steel_id);
        }
        return null;
    }
}