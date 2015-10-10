package com.gulaghad.test.app;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.gulaghad.test.app.SQLiteHelper.PropertyList;
import static com.gulaghad.test.app.SQLiteHelper.StandardList;
import static com.gulaghad.test.app.SQLiteHelper.SteelList;
import static com.gulaghad.test.app.SQLiteHelper.SteelPropertyList;
import static com.gulaghad.test.app.SQLiteHelper.StandardSteelList;
import static com.gulaghad.test.app.SQLiteHelper.Standard;

interface IDataRequester<T> {
    public void dataReady();
}
interface IDataHandler<T> {
    public void setData(T data);
}
interface IDataProvider<T> extends IDataHandler<T> {
    public void requestData(int size);
    public T getData();
}

class BaseRegister<T> {
    private T _register;
    public T get() { return _register; }
    public void set(T data) { _register = data; }
    public void unset(T data) {
        if (_register == data)
            _register = null;
    }
}

public class Register {
    public static boolean queryFocused = false;
    public static String steelFilter = "";
    public static BaseRegister<IDataRequester<SteelList>> steelListDataRequester = new BaseRegister<IDataRequester<SteelList>>();
    public static BaseRegister<IDataProvider<SteelList>> steelListDataProvider = new BaseRegister<IDataProvider<SteelList>>();
    public static int steelId = 0;
    public static BaseRegister<IDataRequester<PropertyList>> propertyListDataRequester = new BaseRegister<IDataRequester<PropertyList>>();
    public static BaseRegister<IDataProvider<PropertyList>> propertyListDataProvider = new BaseRegister<IDataProvider<PropertyList>>();
    public static List<Object> propertySearch = new ArrayList<Object>();
    public static SteelPropertyList.Prop propertyFilter = null;
    public static BaseRegister<IDataRequester<SteelPropertyList>> steelPropertyListDataRequester = new BaseRegister<IDataRequester<SteelPropertyList>>();
    public static BaseRegister<IDataProvider<SteelPropertyList>> steelPropertyListDataProvider = new BaseRegister<IDataProvider<SteelPropertyList>>();
    public static boolean query2Focused = false;
    public static String standardFilter = "";
    public static BaseRegister<IDataRequester<StandardList>> standardListDataRequester = new BaseRegister<IDataRequester<StandardList>>();
    public static BaseRegister<IDataProvider<StandardList>> standardListDataProvider = new BaseRegister<IDataProvider<StandardList>>();
    public static Integer standardId;
    public static BaseRegister<IDataRequester<Pair<Standard, StandardSteelList>>> standardSteelListDataRequester = new BaseRegister<IDataRequester<Pair<Standard, StandardSteelList>>>();
    public static BaseRegister<IDataProvider<Pair<Standard, StandardSteelList>>> standardSteelListDataProvider = new BaseRegister<IDataProvider<Pair<Standard, StandardSteelList>>>();
}