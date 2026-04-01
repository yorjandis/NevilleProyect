package com.ypg.neville.model.db

/**
 * Contrato de tablas/columnas compartido por la app.
 * Ya no implementa SQLiteOpenHelper: la persistencia está en Room.
 */
object DatabaseHelper {
    const val DB_NAME = "neville.db"

    // Tablas
    const val T_Frases = "frases"
    const val T_Videos = "videos"
    const val T_Repo = "repo"
    const val T_Conf = "conf"
    const val T_Apuntes = "notas"

    // Campos comunes
    const val CC_id = "id"
    const val CC_favorito = "fav"
    const val CC_nota = "nota"

    // frases
    const val C_frases_frase = "frase"
    const val C_frases_autor = "autor"
    const val C_frases_fuente = "fuente"
    const val C_frases_in_built = "inbuild"
    const val C_frases_shared = "shared"

    // videos
    const val C_videos_title = "title"
    const val C_videos_link = "link"
    const val C_videos_type = "type"
    const val C_videos_shared = "shared"

    // repo
    const val C_repo_title = "title"
    const val C_repo_link = "link"
    const val C_repo_type = "type"
    const val C_repo_shared = "shared"

    // conf
    const val C_conf_title = "title"
    const val C_conf_link = "link"
    const val C_conf_shared = "shared"

    // notas
    const val C_apunte_title = "title"
}
