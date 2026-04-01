package com.ypg.neville.ui.frag

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.ypg.neville.R
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.GetFromRepo
import com.ypg.neville.model.utils.UiModalWindows

class frag_Setting : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Establecer el tema de la app:
        findPreference<Preference>("tema")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            activity?.recreate()
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
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putString("fuente_frase", numberOnly)
            }
            false
        }

        findPreference<Preference>("fuente_conf")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            var numberOnly = newValue.toString().replace("[^0-9]".toRegex(), "")
            if (numberOnly.isEmpty()) {
                numberOnly = "170"
            }
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                putString("fuente_conf", numberOnly)
            }
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
            startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/donar/".toUri()))
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
            startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/contacto/".toUri()))
            false
        }

        // Lleva a la página de proyecto:
        findPreference<Preference>("web_site")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/".toUri()))
            false
        }

        // Lleva al sitio web en google play, para escribir una reseña
        findPreference<Preference>("resena_app")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val uri = "market://details?id=${requireContext().packageName}".toUri()
            val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(myAppLinkToMarket)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), " unable to find market app", Toast.LENGTH_LONG).show()
            }
            false
        }

        // Muestra el mensaje con las novedades:
        findPreference<Preference>("show_news")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            UiModalWindows.showAyudaContectual(
                requireContext(),
                "Novedades",
                "Que hay de nuevo?",
                getString(R.string.news),
                false,
                AppCompatResources.getDrawable(requireContext(), R.drawable.neville)
            )
            false
        }
    }
}
