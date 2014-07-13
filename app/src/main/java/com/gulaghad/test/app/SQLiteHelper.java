package com.gulaghad.test.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteHelper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "celik.db";
    private static final int DATABASE_VERSION = 1;
    private static final String LOG = "SQLiteHelper";

    // id, (code, country)
    private static SparseArray<Pair<String, String>> _countries = null;
    private final SQLiteDatabase _db;

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        _db = this.getReadableDatabase();
        if (_countries == null)
            _fetchCountries();
    }

    private void _fetchCountries() {
        _countries = new SparseArray<Pair<String, String>>();
        String q = "SELECT id_country, code, country FROM country";
        Log.e(LOG, q);
        Cursor c = _db.rawQuery(q, null);
        if (c.moveToFirst()) {
            do {
                _countries.put(c.getInt(0),
                        Pair.create(c.getString(1), c.isNull(2) ? "" : c.getString(2)));
            } while (c.moveToNext());
        }
    }

    public Pair<Integer, Pair<List<String>, List<Integer>>> fetchSteel(int pos, int size, String filter) {
        List<String> steels = new ArrayList<String>();
        List<Integer> ids = new ArrayList<Integer>();
        int end = pos;
        String q = "SELECT name, id_country, id_steel FROM steelname NATURAL JOIN steel";
        Cursor c;
        if (TextUtils.isEmpty(filter)) {
            q += " GROUP BY id_steel";
            c = _db.rawQuery(q, null);
        } else {
            q += " WHERE name LIKE ?";
            q += " GROUP BY id_steel";
            c = _db.rawQuery(q, new String[]{"%"+filter+"%"});
        }
        Log.i(LOG, q);
        int counter = 0;
        if (c.moveToPosition(pos)) {
            do {
                steels.add(c.getString(0) + " (" + _countries.get(c.getInt(1)).first + ")");
                ids.add(c.getInt(2));
            } while (c.moveToNext() && ++counter < size);
            end = c.getPosition();
        }
        c.close();
        return Pair.create(end, Pair.create(steels, ids));
    }

    public class Element
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
    public List<Element> fetchComposition(Integer steel_id) {
        List<Element> elements = new ArrayList<Element>();
        String q = "SELECT an.id_element, id_min, id_max FROM analyse AS a"
                + " LEFT JOIN analyseproperty AS an ON a.id_analyse == an.id_analyse"
                + " LEFT JOIN element AS e ON an.id_element == e.id_element"
                + " WHERE id_steel == ? ORDER BY e.sort";
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            Log.i("?: ", steel_id.toString());
            do {
                elements.add(new Element(c.getString(0), c.getFloat(1), c.getFloat(2)));
            } while (c.moveToNext());
        }
        c.close();
        return elements;
    }

    public class MechanicalProps
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
        public MechanicalProps(String pstandard, String pdate, String ptitle,
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
    public List<MechanicalProps> fetchMechanicalProps(Integer steel_id) {
        List<MechanicalProps> props = new ArrayList<MechanicalProps>();
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
        //stmtMechanicalProps.bindString(0, steel_id.toString());
        Cursor c = _db.rawQuery(q, new String[]{steel_id.toString()});
        Log.i(LOG, q);
        if (c.moveToFirst()) {
            Log.i("?: ", steel_id.toString());
            do {
                props.add(new MechanicalProps(c.getString(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4),
                        c.getInt(5), c.getInt(6), c.getFloat(7), c.getFloat(8), c.getString(9),
                        c.getString(10), c.getInt(11), c.getInt(12)));
            } while (c.moveToNext());
        }
        c.close();
        return props;
    }

    public class PhysicalProps
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
        public PhysicalProps(String pstandard, String pdate, String ptitle,
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
    public List<PhysicalProps> fetchPhysicalProps(Integer steel_id) {
        List<PhysicalProps> props = new ArrayList<PhysicalProps>();
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
            Log.i("?: ", steel_id.toString());
            do {
                props.add(new PhysicalProps(c.getString(0), c.getString(1), c.getString(2),
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
    public List<HeatTreat> fetchHeatTreat(Integer steel_id) {
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
            Log.i("?: ", steel_id.toString());
            do {
                props.add(new HeatTreat(c.getString(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getString(5),
                        c.getFloat(6), c.getFloat(7), c.getString(8)));
            } while (c.moveToNext());
        }
        c.close();
        return props;
    }
}