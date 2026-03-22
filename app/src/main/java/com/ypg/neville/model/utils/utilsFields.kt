package com.ypg.neville.model.utils

import java.io.File

// Clase para almacenar constantes, campos y flags
object utilsFields {

    // Para notificaciones
    const val NOTIFICACION_ID = 27
    const val NOTIFICACION_CHANNEL_ID = "NEVILLE_CHANNEL"
    const val EMAIL_DEVP = "projectsypg@gmail.com"

    // Para las actualizaciones de la app
    @JvmField
    var isUpdated: Boolean = false // Variable que mantiene el estado de las actualizaciones

    // Info del elemento actualmente cargado
    const val BUNDLE_INFO_KEY_TYPE_ELEMENT = "tipo_elemento" // key para los bundles que serán pasados a cada fragmento y que indicarán el tipo de elemento a cargar
    const val BUNDLE_INFO_KEY_EXTENSION_ELEMENT = "tipo_elemento" // Extensión del elemento actualmente seleccionado
    const val BUNDLE_INFO_KEY_URLPATH_ELEMENT = "tipo_elemento" // Extensión del elemento actualmente seleccionado

    @JvmField
    var ID_row_ofElementLoad: Int = 0 // Almacena el id del elemento actualmente cargado : Para campos de tipo Int

    @JvmField
    var ID_Str_row_ofElementLoad: String = "" // Almacena el id del elemento actualmente cargado : Para Campos de tipo String;

    // Flags:
    @JvmField
    var flagserviceIdBinder: Boolean = false // Si se ha enlazado al servicio es true, de lo contrario es false.

    @JvmField
    var spinnerListInfoItemSelected: String = "" // Almacena el ítem actual en el spinner de frag_list_info

    // Directorios para archivos en el repositorio externo
    const val REPO_DIR_ROOT = "NevilleRepo" // Raíz
    const val REPO_DIR_VIDEOS = "videos" // dir para videos offline
    const val REPO_DIR_AUDIOS = "audios" // dir para audios offline
    @JvmField
    val PATH_ROOT_REPO = File.separator + "sdcard" + File.separator + REPO_DIR_ROOT + File.separator // Directorio padre para Repositorio

    // Fragment Setting - Variables de configuración
    // Campos para Almacenar los elementos a cargar al inicio
    const val SETTING_KEY_ID_ULTIMA_FRASE = "id_last_Frase_loaded"
    const val SETTING_KEY_ULTIMA_CONFERENCIA = "id_last_conf_loaded"

    const val SETTING_KEY_CONF_SCROLL_POSITION = "scroll_position_confe" // Almacena la posición de las conferencias

    // Enum para consultas específicas a la BD
    enum class consultasBD {
        NADA, TODAS_LAS_FRASES, FRASES_INBUILT
    }
}
