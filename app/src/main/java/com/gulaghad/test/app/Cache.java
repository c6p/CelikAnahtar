package com.gulaghad.test.app;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Cache {
    public static Map<String, SQLiteHelper.SteelList> steelList = new TreeMap<String, SQLiteHelper.SteelList>();
    public static Map<String, SQLiteHelper.StandardList> standardList = new TreeMap<String, SQLiteHelper.StandardList>();
    public static Map<Integer, SQLiteHelper.PropertyList> propertyList = new HashMap<Integer, SQLiteHelper.PropertyList>();
    public static SQLiteHelper.SteelPropertyList steelPropertyList;
}
