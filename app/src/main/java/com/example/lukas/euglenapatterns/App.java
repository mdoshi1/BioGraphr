package com.example.lukas.euglenapatterns;


import android.app.Application;
import android.content.Context;

public class App extends Application {

    // Stores a Context variable
    private static Context mContext;

    // Returns mContext
    public static Context getContext() {
        return mContext;
    }

    // Sets mContext
    public static void setContext(Context context) { mContext = context; }

}