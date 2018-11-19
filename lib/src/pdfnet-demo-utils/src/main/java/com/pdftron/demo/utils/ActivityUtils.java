package com.pdftron.demo.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.content.res.AppCompatResources;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;

public class ActivityUtils {

    public static void setupDrawer(Context context, DrawerLayout drawerLayout, NavigationView navigationView, ConstraintLayout drawerHeader) {
        // Set up the drawer.
        if (Utils.isLollipop()) {
            // Ensure the drawers are set to use the recommended material design elevation.
            drawerLayout.setDrawerElevation(context.getResources().getDimension(R.dimen.drawer_elevation));

            /*
            The DrawerLayout's fake status bar background is not sufficient for our combination
            of drawers and fullscreen Views. The status bar needs to be drawn above the fullscreen
            content, but below the drawers.

            (DrawerLayout draws its status bar background below everything)

            A {@code null} background will not be drawn.
            */
            drawerLayout.setStatusBarBackground(null);
        }
        ViewCompat.setFitsSystemWindows(drawerLayout, Utils.isLollipop());

        ViewCompat.setOnApplyWindowInsetsListener(drawerHeader, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                // Skip the bottom inset.
                ViewCompat.onApplyWindowInsets(v,
                        insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                                insets.getSystemWindowInsetRight(), 0));
                return insets;
            }
        });

        ColorStateList iconTintList = AppCompatResources.getColorStateList(context, R.color.selector_color_drawer_icon);
        navigationView.setItemIconTintList(iconTintList);
        navigationView.addHeaderView(drawerHeader);

        MenuItem externalStorageItem = navigationView.getMenu().findItem(R.id.item_external_storage);
        if (externalStorageItem != null) {
            externalStorageItem.setVisible(Utils.isLollipop());
        }
    }
    public static void setupDrawer(Context context, DrawerLayout drawerLayout, NavigationView navigationView, LinearLayout drawerHeader) {
        // Set up the drawer.
        if (Utils.isLollipop()) {
            // Ensure the drawers are set to use the recommended material design elevation.
            drawerLayout.setDrawerElevation(context.getResources().getDimension(R.dimen.drawer_elevation));

            /*
            The DrawerLayout's fake status bar background is not sufficient for our combination
            of drawers and fullscreen Views. The status bar needs to be drawn above the fullscreen
            content, but below the drawers.

            (DrawerLayout draws its status bar background below everything)

            A {@code null} background will not be drawn.
            */
            drawerLayout.setStatusBarBackground(null);
        }
        ViewCompat.setFitsSystemWindows(drawerLayout, Utils.isLollipop());

        ViewCompat.setOnApplyWindowInsetsListener(drawerHeader, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                // Skip the bottom inset.
                ViewCompat.onApplyWindowInsets(v,
                        insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                                insets.getSystemWindowInsetRight(), 0));
                return insets;
            }
        });

        ColorStateList iconTintList = AppCompatResources.getColorStateList(context, R.color.selector_color_drawer_icon);
        navigationView.setItemIconTintList(iconTintList);
        navigationView.addHeaderView(drawerHeader);

        MenuItem externalStorageItem = navigationView.getMenu().findItem(R.id.item_external_storage);
        if (externalStorageItem != null) {
            externalStorageItem.setVisible(Utils.isLollipop());
        }
    }

}
