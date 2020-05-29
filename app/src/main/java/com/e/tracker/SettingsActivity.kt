package com.e.tracker

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_osm.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)
        setSupportActionBar(this.toolbar)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            //bindPreferenceSummaryToValue(findPreference( getString(R.string.gpx_distance_key)) )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            if (item.itemId == android.R.id.home) {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }


    companion object {


        private fun bindPreferenceSummaryToValue(preference: Preference?) {
            // set the listener to watch for value changes
            if (preference != null) {
                preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener


                // trigger listener with preferences current values
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, ""))
                }
        }

        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, newValue ->

            val stringValue = newValue.toString()

            if (preference is ListPreference ) {
                // look up the correct display value in preference's entries list
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // set summary to reflect new value
                preference.setSummary(
                    if (index >= 0)
                        listPreference.entries[index]
                    else
                        null
                )
            } else {
                preference.summary = stringValue
            }

            true
        }
    }
}