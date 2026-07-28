package com.ypg.neville.feature.healingcenter.domain

import android.content.Context
import com.ypg.neville.R
import java.util.Locale

enum class HealingEmergencyContactKind { EMERGENCY, EMOTIONAL_SUPPORT }

data class HealingEmergencyContact(
    val id: String,
    val title: String,
    val number: String,
    val detail: String,
    val kind: HealingEmergencyContactKind,
    val sourceUrl: String?
) {
    val dialableNumber: String
        get() = number.filter { it.isDigit() || it == '+' }
}

data class HealingEmergencyResources(
    val regionCode: String,
    val countryName: String,
    val contacts: List<HealingEmergencyContact>,
    val note: String
)

class HealingEmergencyResourceProvider(private val context: Context) {
    private val locale: Locale
        get() = context.resources.configuration.locales[0]

    val detectedRegionCode: String
        get() = locale.country.takeIf(String::isNotBlank)?.uppercase() ?: "ES"

    val availableRegionCodes: List<String>
        get() = (EUROPEAN_112_REGIONS + setOf("AU", "CA", "GB", "IE", "MX", "NZ", "US", detectedRegionCode))
            .sortedBy(::countryName)

    fun countryName(regionCode: String): String {
        val display = Locale.Builder().setRegion(regionCode.uppercase()).build().getDisplayCountry(locale)
        return display.takeIf(String::isNotBlank) ?: regionCode.uppercase()
    }

    fun resources(regionCode: String): HealingEmergencyResources {
        val code = regionCode.uppercase()
        val emergencyNumber = emergencyNumber(code)
        val contacts = buildList {
            if (emergencyNumber != null) {
                add(
                    HealingEmergencyContact(
                        id = "$code-emergency",
                        title = context.getString(R.string.healing_emergencies),
                        number = emergencyNumber,
                        detail = context.getString(R.string.healing_emergency_detail),
                        kind = HealingEmergencyContactKind.EMERGENCY,
                        sourceUrl = emergencySourceUrl(code)
                    )
                )
            }
            addAll(emotionalSupportContacts(code))
        }
        val note = if (emergencyNumber == null) {
            context.getString(R.string.healing_region_unverified)
        } else {
            context.getString(R.string.healing_region_detection_note)
        }
        return HealingEmergencyResources(code, countryName(code), contacts, note)
    }

    private fun emergencyNumber(code: String): String? = when {
        code in EUROPEAN_112_REGIONS -> "112"
        code in setOf("US", "CA", "MX") -> "911"
        code == "GB" -> "999"
        code == "AU" -> "000"
        code == "NZ" -> "111"
        else -> null
    }

    private fun emotionalSupportContacts(code: String): List<HealingEmergencyContact> = when (code) {
        "ES" -> listOf(support(
            code, "024", "Línea 024", "024",
            context.getString(R.string.healing_contact_spain_024),
            "https://www.sanidad.gob.es/linea024/home.htm"
        ))
        "US" -> listOf(support(
            code, "988", "988 Suicide & Crisis Lifeline", "988",
            context.getString(R.string.healing_contact_call_text_crisis),
            "https://988lifeline.org"
        ))
        "CA" -> listOf(support(
            code, "988", "9-8-8 Suicide Crisis Helpline", "988",
            context.getString(R.string.healing_contact_call_text_crisis),
            "https://988.ca"
        ))
        "GB", "IE" -> listOf(support(
            code, "samaritans", "Samaritans", "116 123",
            context.getString(R.string.healing_contact_samaritans),
            "https://www.samaritans.org/how-we-can-help/contact-samaritan/"
        ))
        "AU" -> listOf(support(
            code, "lifeline", "Lifeline Australia", "13 11 14",
            context.getString(R.string.healing_contact_lifeline_au),
            "https://www.lifeline.org.au/131114/"
        ))
        "NZ" -> listOf(support(
            code, "1737", "1737, Need to talk?", "1737",
            context.getString(R.string.healing_contact_nz_1737),
            "https://1737.org.nz"
        ))
        "MX" -> listOf(support(
            code, "linea-vida", "Línea de la Vida", "800 911 2000",
            context.getString(R.string.healing_contact_mexico),
            "https://www.gob.mx/lineadelavida"
        ))
        else -> emptyList()
    }

    private fun support(
        code: String,
        id: String,
        title: String,
        number: String,
        detail: String,
        source: String
    ) = HealingEmergencyContact(
        id = "$code-$id",
        title = title,
        number = number,
        detail = detail,
        kind = HealingEmergencyContactKind.EMOTIONAL_SUPPORT,
        sourceUrl = source
    )

    private fun emergencySourceUrl(code: String): String? = when {
        code in EUROPEAN_112_REGIONS -> "https://digital-strategy.ec.europa.eu/en/policies/112"
        code == "US" -> "https://www.911.gov"
        code == "CA" -> "https://www.canada.ca/en/public-health/services/mental-health-services/mental-health-get-help.html"
        code == "GB" -> "https://www.gov.uk/guidance/999-and-112-the-uks-national-emergency-numbers"
        code == "AU" -> "https://www.health.gov.au/form/general-enquiries"
        code == "NZ" -> "https://www.govt.nz/browse/health/help-in-a-crisis/emergencies/"
        code == "MX" -> "https://www.gob.mx/911"
        else -> null
    }

    companion object {
        const val INTERNATIONAL_DIRECTORY_URL = "https://findahelpline.com/"

        private val EUROPEAN_112_REGIONS = setOf(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DE", "DK", "EE", "ES",
            "FI", "FR", "GR", "HU", "IE", "IS", "IT", "LI", "LT", "LU",
            "LV", "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK"
        )
    }
}
