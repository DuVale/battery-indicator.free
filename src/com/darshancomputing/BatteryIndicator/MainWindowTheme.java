/*
    Copyright (c) 2010 Josiah Barber (aka Darshan)

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

import android.content.res.Resources;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;

public class MainWindowTheme {
    private float density;
    private Resources res;

    public Theme theme;

    public MainWindowTheme(String themeName, DisplayMetrics metrics, Resources r) {
        density = metrics.density;
        res = r;

        theme = new DefaultTheme();
    }

    public abstract class Theme {
        public int mainFrameLayout;

        public int mainLayoutPaddingLeft;
        public int mainLayoutPaddingTop;
        public int mainLayoutPaddingRight;
        public int mainLayoutPaddingBottom;

    }

    private class DefaultTheme extends Theme {
        public DefaultTheme() {
            mainFrameLayout = R.layout.main_frame_default;

            int[] mainLayoutPadding = res.getIntArray(R.array.theme_default_main_layout_padding);

            mainLayoutPaddingLeft = (int) (mainLayoutPadding[0] * density);
            mainLayoutPaddingTop = (int) (mainLayoutPadding[1] * density);
            mainLayoutPaddingRight = (int) (mainLayoutPadding[2] * density);
            mainLayoutPaddingBottom = (int) (mainLayoutPadding[3] * density);
        }
    }
}
