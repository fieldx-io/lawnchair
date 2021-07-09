/*
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

package ch.deletescape.lawnchair.sesame.preferences

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference
import ch.deletescape.lawnchair.sesame.Sesame
import ninja.sesame.lib.bridge.v1.SesameFrontend
import android.content.pm.ApplicationInfo

import java.util.HashSet

import java.util.Collections

import android.content.pm.PackageManager
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import ch.deletescape.lawnchair.applyAccent
import ch.deletescape.lawnchair.preferences.StyledIconPreference
import com.android.launcher3.R

import java.util.ArrayList


class EnableGoogleVersionPreference(context: Context, attrs: AttributeSet) : StyledIconPreference(context, attrs) {
    init {
        summary = "Click to Enable Google Play Store and other Google Apps"
        if (context != null) {
            val enabled = getDisabledApps(context)?.contains("com.android.vending")
            if (enabled == false) {
                summary = "Google Services already enabled on this device"
            }
        }
    }

    override fun onClick() {
        super.onClick()

        val enabled = getDisabledApps(context)?.contains("com.android.vending")
        if (enabled == false) {
            Toast.makeText(context, "Google services already enabled", Toast.LENGTH_SHORT).show()
        } else AlertDialog.Builder(context)
            .setTitle("Instructions")
            .setMessage("Click OK to Open Ideostore and download 'Google Services Enabler' app to enable Play Store")
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, i: Int ->
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.component = ComponentName.unflattenFromString("io.fieldx.store/io.android.store.ui.MainActivity")
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel"){ dialogInterface: DialogInterface, i: Int ->
                    //Do nothing
            }
            .setCancelable(true)
            .show().applyAccent()
    }


    fun getDisabledApps(context: Context): List<String>? {
        val mPackageManager = context.packageManager
        // Disabled system apps list = {All system apps} - {Enabled system apps}
        val disabledSystemApps: MutableList<String> = ArrayList()
        // This list contains both enabled and disabled apps.
        val allApps = mPackageManager.getInstalledApplications(
            PackageManager.GET_UNINSTALLED_PACKAGES
                                                              )
        Collections.sort(allApps, ApplicationInfo.DisplayNameComparator(mPackageManager))
        // This list contains all enabled apps.
        val enabledApps = mPackageManager.getInstalledApplications(0 /* Default flags */)
        val enabledAppsPkgNames: MutableSet<String> = HashSet()
        for (applicationInfo in enabledApps) {
            enabledAppsPkgNames.add(applicationInfo.packageName)
        }
        for (applicationInfo in allApps) {
            // Interested in disabled system apps only.
            if (!enabledAppsPkgNames.contains(applicationInfo.packageName)
                && applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            ) {
                disabledSystemApps.add(applicationInfo.packageName)
            }
        }
        return disabledSystemApps
    }
}