package com.tierep.notificationanalyser.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.tierep.notificationanalyser.R;
import com.tierep.notificationanalyser.models.Application;
import com.tierep.notificationanalyser.models.ApplicationDao;
import com.tierep.notificationanalyser.models.DatabaseHelper;

import java.sql.SQLException;
import java.util.List;

import de.timroes.android.listview.EnhancedListView;

/**
 * An activity that will display all the ignored applications in the statistics.
 */
public class IgnoredApps extends Activity {
    private DatabaseHelper databaseHelper = null;
    private ApplicationIgnoreAdapter ignoredAppsAdapter = null;
    private EnhancedListView listView = null;

    public DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignored_apps);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        listView = (EnhancedListView) findViewById(R.id.list_ignored_apps);
        try {
            List<Application> applicationList = getDatabaseHelper().getApplicationDao().getIgnoredApps();
            applicationList.add(0, new Application()); // Insert empty element for header
            ignoredAppsAdapter = new ApplicationIgnoreAdapter(this, applicationList);
            listView.setAdapter(ignoredAppsAdapter);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        listView.setDismissCallback(new EnhancedListView.OnDismissCallback() {
            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView enhancedListView, final int i) {
                final Application item = ignoredAppsAdapter.getItem(i);
                ignoredAppsAdapter.remove(item);

                return new EnhancedListView.Undoable() {
                    @Override
                    public void undo() {
                        ignoredAppsAdapter.insert(item, i);
                    }

                    @Override
                    public void discard() {
                        try {
                            ApplicationDao dao = getDatabaseHelper().getApplicationDao();
                            item.setIgnore(false);
                            dao.update(item);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                };
            }
        });
        listView.setRequireTouchBeforeDismiss(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        listView.discardUndo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    private class ApplicationIgnoreAdapter extends ArrayAdapter<Application> {
        public ApplicationIgnoreAdapter(Context context, List<Application> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View listViewHeader = inflater.inflate(R.layout.list_ignored_apps_header, null, false);

                if (ignoredAppsAdapter.getCount() == 1) {
                    TextView ignoredAppsDescription = (TextView) listViewHeader.findViewById(R.id.ignored_apps_description);
                    ignoredAppsDescription.setText(R.string.ignored_apps_no_items);
                }

                return listViewHeader;
            } else {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.list_ignored_apps_el, parent, false);
                TextView appName = (TextView) view.findViewById(R.id.ignored_app_name);
                ImageView appImage = (ImageView) view.findViewById(R.id.ignored_app_image);

                Application app = this.getItem(position);
                PackageManager packageManager = getContext().getPackageManager();
                String str_appName = null;
                Drawable icon = null;

                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(app.getPackageName(), 0);
                    str_appName = packageManager.getApplicationLabel(appInfo).toString();
                    icon = packageManager.getApplicationIcon(appInfo);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (str_appName != null) appName.setText(str_appName);
                else appName.setText(app.getPackageName());
                if (icon != null) appImage.setImageDrawable(icon);

                view.findViewById(R.id.ignored_app_delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listView.delete(position);
                    }
                });

                return view;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}
