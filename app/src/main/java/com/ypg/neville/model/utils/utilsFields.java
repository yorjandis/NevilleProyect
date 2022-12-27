package com.ypg.neville.model.utils;

import java.io.File;


//Clase para almacenar contantes, campos y flags
public class utilsFields {



//Para notificaciones
    public static final int NOTIFICACION_ID             = 27;
    public static final String NOTIFICACION_CHANNEL_ID  = "NEVILLE_CHANNEL";
    public static final String EMAIL_DEVP               = "projectsypg@gmail.com";

    //Para las actualizaciones de la app
    public static Boolean isUpdated = false;          //Variable que mantiene el estado de las actualizaciones

    //Info del elemento actualmente cargado
    public static String BUNDLE_INFO_KEY_TYPE_ELEMENT       = "tipo_elemento"; // key para los bundles que serán pasados a cada fragmento y que indicarán el tipo de elemento a cargar (biografia,galeriafotos,Abdullah,preg,cita,ayudas,video_gredd,librosADescargar
    public static String BUNDLE_INFO_KEY_EXTENSION_ELEMENT  = "tipo_elemento"; // Extensión del elemento actualmente seleccionado
    public static String BUNDLE_INFO_KEY_URLPATH_ELEMENT    = "tipo_elemento"; // Extensión del elemento actualmente seleccionado

    public static int ID_row_ofElementLoad;                     //Almacena el id del elemento actualmente cargado : Para campos de tipo Int
    public static String ID_Str_row_ofElementLoad = "";         //Almacena el id del elemento actualmente cargado : Para Campos de tipo String;
    //public static String elementLoaded = "";                    //Almacena el tipo de elemento actualmente cargado (biografia,galeriafotos,Abdullah,preg,cita,ayudas,video_gredd,librosADescargar
                                                                // frases,conf,video_conf,video_book, video_ext,audio_ext)

    //Flags:
    public  static boolean flagserviceIdBinder  = false;        //Si se ha enlazado al servicio es true, de lo ocntrario es false.



    public static String spinnerListInfoItemSelected = "";         //Almacena el iten actual en el spinner de frag_list_info


    // Directorios para  archivos  en el repositorio externo
    public static final     String REPO_DIR_ROOT        = "NevilleRepo"; //Raíz
    public static final     String REPO_DIR_VIDEOS      = "videos"; //dir para videos offline
    public static final     String REPO_DIR_AUDIOS      = "audios"; //dir para audios offline
    public static final     String PATH_ROOT_REPO       = File.separator + "sdcard" + File.separator + utilsFields.REPO_DIR_ROOT + File.separator;  //Directorio padre para Repositorio



//Fragment Setting -  Variables de configuración
    //Campos para Almacenar los elementos a cargar al inicio
    public static final String SETTING_KEY_ID_ULTIMA_FRASE      = "id_last_Frase_loaded";
    public static final String SETTING_KEY_ULTIMA_CONFERENCIA   = "id_last_conf_loaded";

    public static final String SETTING_KEY_CONF_SCROLL_POSITION = "scroll_position_confe"; //Almacena la posición de las conferencias

    //Frag: constantes para elcontro de la reproducción en streaming:
        //: flag para activar y desactivar el reproductor:
        //: Flag de switch para a activaci´pn / desacticacion

}