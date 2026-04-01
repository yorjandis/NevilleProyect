package com.ypg.neville.Ui.frag

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.GetFromRepo
import com.ypg.neville.model.utils.UiModalWindows

class frag_Setting : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Establecer el tema de la app:
        findPreference<Preference>("tema")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            activity?.let {
                it.startActivity(Intent(context, MainActivity::class.java))
                it.finish()
                it.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            true
        }

        // Tamaño de fuente:
        findPreference<Preference>("fuente_frase")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            var numberOnly = newValue.toString().replace("[^0-9]".toRegex(), "")
            if (numberOnly.isEmpty()) {
                numberOnly = "28"
            }
            if (numberOnly.toInt() >= 40) {
                numberOnly = "28"
            }
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("fuente_frase", numberOnly).apply()
            false
        }

        findPreference<Preference>("fuente_conf")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            var numberOnly = newValue.toString().replace("[^0-9]".toRegex(), "")
            if (numberOnly.isEmpty()) {
                numberOnly = "170"
            }
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("fuente_conf", numberOnly).apply()
            false
        }

        // Color de los marcos de la app
        findPreference<Preference>("color_marcos")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ColorPickerManager.showColorPicker(requireContext(), 0, "color_marcos", "Color de Marcos")
            false
        }

        // Color del texto de las frases
        findPreference<Preference>("color_letra_frases")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ColorPickerManager.showColorPicker(requireContext(), 0, "color_letra_frases", "Color de letra en frases")
            false
        }

        // Lleva a la página de proyecto, sección donar:
        findPreference<Preference>("donar")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://projectsypg.mozello.com/donar/")))
            false
        }

        // Actualizar el listado de frases con la información desde la web
        findPreference<Preference>("update_frases_from_web")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            GetFromRepo.getFrasesFromWeb(requireContext())
            Toast.makeText(requireContext(), "El compendio de frases se esta actualizando", Toast.LENGTH_SHORT).show()
            false
        }

        // Lleva a la página de proyecto, sección escribir comentario:
        findPreference<Preference>("write_comment")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://projectsypg.mozello.com/contacto/")))
            false
        }

        // Lleva a la página de proyecto:
        findPreference<Preference>("web_site")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://projectsypg.mozello.com/")))
            false
        }

        // Lleva al sitio web en google play, para escribir una reseña
        findPreference<Preference>("resena_app")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val uri = Uri.parse("market://details?id=" + requireContext().packageName)
            val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(myAppLinkToMarket)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), " unable to find market app", Toast.LENGTH_LONG).show()
            }
            false
        }

        // Muestra el mensaje con las novedades:
        findPreference<Preference>("show_news")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            UiModalWindows.showAyudaContectual(requireContext(), "Novedades", "Que hay de nuevo?", getString(R.string.news), false, requireContext().getDrawable(R.drawable.neville))
            false
        }
    }
}
