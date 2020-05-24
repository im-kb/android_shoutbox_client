package com.example.ppsm_budzik_shoutbox.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.example.ppsm_budzik_shoutbox.R
import com.example.ppsm_budzik_shoutbox.navView
import com.example.ppsm_budzik_shoutbox.ui.shoutbox.ShoutboxFragment

class SettingsFragment : Fragment() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var loginInput: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingsViewModel =
            ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        val loginButton = root.findViewById<Button>(R.id.loginButton)
        loginInput = root.findViewById(R.id.loginInput)

        loadLogin()

        loginButton.setOnClickListener {
            saveLogin()
            navView.setCheckedItem(R.id.nav_shoutbox)

            val fragment: Fragment = ShoutboxFragment()
            val fragmentManager: FragmentManager? = fragmentManager
            fragmentManager?.beginTransaction()
                ?.replace(R.id.nav_host_fragment, fragment)
                ?.commit()
        }
        return root
    }

    private fun saveLogin() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.saved_login), loginInput.text.toString())
            commit()
        }
    }

    private fun loadLogin() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val defaultValue = resources.getString(R.string.saved_login)
        loginInput.setText(sharedPref.getString(getString(R.string.saved_login), defaultValue))
    }
}
