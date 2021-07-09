/*
 *     Copyright (C) 2021 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Created by Amit S.
 */
public class FieldXContentProviderAPI {
    
    public static final Gson gson = new Gson();
    private static final String FieldXContentURI = "content://io.fieldx.content.provider/action";
    private static final int INSTALL_APP = 26;
    private static final int HIDE_APP = 25;
    
    private static Uri getFieldXUri(String actionId) {
        return Uri.parse(FieldXContentURI + "/" + actionId);
    }
    
    public static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        } catch (Exception ignore) {
        }
        return false;
    }
    
    private static String query(Context context, Uri uri, String message) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{}, message, null, "ORDER");
            if (cursor.getCount()==0 || cursor.getCount() > 1) {
                return null;
            }
            cursor.moveToNext();
            String response = cursor.getString(cursor.getColumnIndex("response"));
//            CPResponse<String> r = gson.fromJson(response, new TypeToken<CPResponse<String>>() {
//            }.getType());
//            if (r!=null && r.getCode()==200) {
//                return r.getMessage();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor!=null) {
                cursor.close();
            }
        }
        return null;
    }
    
    public static void showApp(Context context, String selectedApp) {
        String response = query(context, getFieldXUri("" + INSTALL_APP), selectedApp);
    }
    
    public static void hideApp(Context context, String selectedApp) {
        String response = query(context, getFieldXUri("" + HIDE_APP), selectedApp);
    }
}
