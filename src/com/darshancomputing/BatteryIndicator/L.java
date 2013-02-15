/*
    Copyright (c) 2013 Josiah Barber (aka Darshan)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.darshancomputing.BatteryIndicator;

import android.util.Log;

public class L {
    private String tag;

    public L(String t) {
        tag = t;
    }

    private static String timeStamp(String s) {
        return "" + System.currentTimeMillis() + "ms: " + s;
    }

    public void d(String s) {
        Log.d(tag, timeStamp(s));
    }

    public void e(String s) {
        Log.e(tag, timeStamp(s));
    }

    public void i(String s) {
        Log.i(tag, timeStamp(s));
    }
}
