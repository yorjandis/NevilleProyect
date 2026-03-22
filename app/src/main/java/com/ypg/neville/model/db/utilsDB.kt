package com.ypg.neville.model.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.utils.GetFromRepo
import com.ypg.neville.model.utils.utilsFields
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.Objects
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

// Clase que se encarga de realizar operaciones específicas y de ayuda a la BD
object utilsDB {

    /**
     * Pasa las frases (inbuilt) de xml a la tabla frases de sqlite
     */
    @JvmStatic
    fun yor_populateFraseTable(context: Context) {
        val dbManager = DBManager(context).open()
        dbManager.DeleteTable(DatabaseHelper.T_Frases)
        dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Frases)

        val listFrases = GetFromRepo.getFrasesFromXML(context)
        for (frase in listFrases) {
            val temp = frase.split("::").toTypedArray()
            insertNewFrase(context, temp[1], temp[0], "", "1")
        }
        dbManager.close()
    }

    /**
     * Pasa la información de los videos de xml a la tabla videos en sqlite
     */
    @JvmStatic
    fun yor_populateVideosTable(context: Context) {
        val dbManager = DBManager(context).open()
        dbManager.DeleteTable(DatabaseHelper.T_Videos)
        dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Videos)

        val arrayVideoYoutube = GetFromRepo.getUrlsVideosFromXML(context)
        val contentValues = ContentValues()

        for (video in arrayVideoYoutube) {
            val temp = video.split("::").toTypedArray()
            contentValues.put(DatabaseHelper.C_videos_link, temp[0])
            contentValues.put(DatabaseHelper.C_videos_title, temp[1])
            contentValues.put(DatabaseHelper.C_videos_type, "conf")
            contentValues.put(DatabaseHelper.CC_favorito, "0")
            contentValues.put(DatabaseHelper.CC_nota, "")
            contentValues.put(DatabaseHelper.C_videos_shared, "0")
            dbManager.insert(DatabaseHelper.T_Videos, contentValues)
            contentValues.clear()
        }

        val arrayAudioLibrosYoutube = context.resources.getStringArray(R.array.list_audiolibros)
        for (audioLibro in arrayAudioLibrosYoutube) {
            val temp = audioLibro.split("::").toTypedArray()
            contentValues.put(DatabaseHelper.C_videos_link, temp[0])
            contentValues.put(DatabaseHelper.C_videos_title, temp[1])
            contentValues.put(DatabaseHelper.C_videos_type, "audioLibro")
            contentValues.put(DatabaseHelper.CC_favorito, "0")
            contentValues.put(DatabaseHelper.CC_nota, "")
            contentValues.put(DatabaseHelper.C_videos_shared, "0")
            dbManager.insert(DatabaseHelper.T_Videos, contentValues)
            contentValues.clear()
        }

        val arrayVideosGreggYoutube = context.resources.getStringArray(R.array.list_gregg_videos)
        for (videoGregg in arrayVideosGreggYoutube) {
            val temp = videoGregg.split("::").toTypedArray()
            contentValues.put(DatabaseHelper.C_videos_link, temp[0])
            contentValues.put(DatabaseHelper.C_videos_title, temp[1])
            contentValues.put(DatabaseHelper.C_videos_type, "gregg")
            contentValues.put(DatabaseHelper.CC_favorito, "0")
            contentValues.put(DatabaseHelper.CC_nota, "")
            contentValues.put(DatabaseHelper.C_videos_shared, "0")
            dbManager.insert(DatabaseHelper.T_Videos, contentValues)
            contentValues.clear()
        }
        dbManager.close()
    }

    /**
     * Pasa el contenido de las conferencias inbuilt a la tabla conf
     */
    @JvmStatic
    @Throws(IOException::class)
    fun yor_populateConfTable(context: Context) {
        val dbManager = DBManager(context).open()
        dbManager.DeleteTable(DatabaseHelper.T_Conf)
        dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Conf)

        val contentValues = ContentValues()
        val fileList = GetFromRepo.getConfListFromAssets(context)

        if (fileList != null) {
            for (file in fileList) {
                contentValues.put(DatabaseHelper.C_conf_title, file.replace(".txt", ""))
                contentValues.put(DatabaseHelper.C_conf_link, file)
                contentValues.put(DatabaseHelper.CC_favorito, "0")
                contentValues.put(DatabaseHelper.CC_nota, "")
                contentValues.put(DatabaseHelper.C_conf_shared, "0")
                dbManager.insert(DatabaseHelper.T_Conf, contentValues)
                contentValues.clear()
            }
        }
        dbManager.close()
    }

    /**
     * Restaurar automaticamente la información de las tablas si no existe
     */
    @JvmStatic
    fun RestoreDBInfo(context: Context): Boolean {
        var result = false
        val dbManager = DBManager(context).open()

        var cursor = dbManager.ejectSQLRawQuery("SELECT * FROM ${DatabaseHelper.T_Frases};")
        if (cursor.count == 0) {
            yor_populateFraseTable(context)
            result = true
        }
        cursor.close()

        cursor = dbManager.ejectSQLRawQuery("SELECT * FROM ${DatabaseHelper.T_Conf};")
        if (cursor.count == 0) {
            try {
                yor_populateConfTable(context)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            result = true
        }
        cursor.close()

        cursor = dbManager.ejectSQLRawQuery("SELECT * FROM ${DatabaseHelper.T_Videos};")
        if (cursor.count == 0) {
            yor_populateVideosTable(context)
            result = true
        }
        cursor.close()

        dbManager.close()
        return result
    }

    /**
     * Actualiza el contenido de las tablas audios_ext, videos_ext a partir del almacenamiento externo.
     */
    @JvmStatic
    fun popularDB_Repo(context: Context) {
        val countAudios = AtomicInteger()
        val countVideos = AtomicInteger()

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val dbManager = DBManager(context).open()
            val contentValues = ContentValues()

            dbManager.DeleteTable(DatabaseHelper.T_Repo)
            dbManager.CreateTable(DatabaseHelper.CREATE_TABLE_Repo)

            val dirPathVideos = Environment.getExternalStorageDirectory().toString() + File.separator + utilsFields.REPO_DIR_ROOT + File.separator + utilsFields.REPO_DIR_VIDEOS
            val dirPathAudios = Environment.getExternalStorageDirectory().toString() + File.separator + utilsFields.REPO_DIR_ROOT + File.separator + utilsFields.REPO_DIR_AUDIOS
            val fVideos = File(dirPathVideos)
            val fAudios = File(dirPathAudios)

            val filesVideos = fVideos.listFiles()
            val filesAudios = fAudios.listFiles()

            if (filesVideos != null && filesVideos.isNotEmpty()) {
                countVideos.addAndGet(filesVideos.size)
                for (file in filesVideos) {
                    val temp = file.toString().split("/").toTypedArray()
                    contentValues.put("title", temp[temp.size - 1])
                    contentValues.put("link", temp[temp.size - 1])
                    contentValues.put("type", "video")
                    dbManager.insert(DatabaseHelper.T_Repo, contentValues)
                    contentValues.clear()
                }
            }

            if (filesAudios != null && filesAudios.isNotEmpty()) {
                countAudios.addAndGet(filesAudios.size)
                for (file in filesAudios) {
                    val temp = file.toString().split("/").toTypedArray()
                    contentValues.put("title", temp[temp.size - 1])
                    contentValues.put("link", temp[temp.size - 1])
                    contentValues.put("type", "audio")
                    dbManager.insert(DatabaseHelper.T_Repo, contentValues)
                    contentValues.clear()
                }
            }
            dbManager.close()

            if (context is MainActivity) {
                if (context.isFinishing) return@execute
            }

            handler.post {
                Toast.makeText(context, "Se actualizaron en total: ${countAudios.get()} audios ${countVideos.get()} videos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JvmStatic
    fun indexOffLineMedios(context: Context) {
        val md5Result = dirCheckSum()
        if (md5Result == 0) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val temp = prefs.getInt("dir_checksum", 0)

        if (temp == 0 || temp != md5Result) {
            prefs.edit().putInt("dir_checksum", md5Result).apply()
            try {
                popularDB_Repo(context)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun dirCheckSum(): Int {
        var md5DirVideos = 0
        var md5DirAudios = 0
        var resultError = false

        try {
            val folderVideos = File(utilsFields.PATH_ROOT_REPO + utilsFields.REPO_DIR_VIDEOS)
            val filesVideos = folderVideos.listFiles()
            if (filesVideos != null && filesVideos.isNotEmpty()) {
                for (file in filesVideos) {
                    md5DirVideos += file.hashCode()
                }
            }
        } catch (ignored: Exception) {
            resultError = true
        }

        try {
            val folderAudios = File(utilsFields.PATH_ROOT_REPO + utilsFields.REPO_DIR_AUDIOS)
            val filesAudios = folderAudios.listFiles()
            if (filesAudios != null && filesAudios.isNotEmpty()) {
                for (file in filesAudios) {
                    md5DirAudios += file.hashCode()
                }
            }
        } catch (ignored: Exception) {
            resultError = true
        }

        if (resultError) return 0
        return Math.abs(md5DirVideos) + Math.abs(md5DirAudios)
    }

    @JvmStatic
    fun LoadRepoFromDB(context: Context, DirRepo: String, lista: MutableList<String>): Int {
        var result = 0
        val dbManager = DBManager(context).open()
        val cursor = dbManager.ejectSQLRawQuery("SELECT title FROM ${DatabaseHelper.T_Repo} WHERE ${DatabaseHelper.C_repo_type}='$DirRepo';")
        lista.clear()
        if (cursor.moveToFirst()) {
            result = cursor.count
            while (!cursor.isAfterLast) {
                val temp = cursor.getString(0).split("/").toTypedArray()
                lista.add(temp[temp.size - 1])
                cursor.moveToNext()
            }
        }
        cursor.close()
        dbManager.close()
        return result
    }

    @JvmStatic
    fun readFavState(context: Context, TableName: String, ColumnID: String, id: String): String {
        var result = ""
        val dbManager = DBManager(context).open()
        val cursor = dbManager.ejectSQLRawQuery("SELECT ${DatabaseHelper.CC_favorito} FROM $TableName WHERE $ColumnID='$id';")
        if (cursor.moveToFirst()) {
            result = cursor.getString(0)
        }
        cursor.close()
        dbManager.close()
        return result
    }

    @JvmStatic
    fun UpdateFavorito(context: Context, TableName: String, ColumnID: String, id_str: String, id_int: Int): String {
        val dbManager = DBManager(context).open()
        var result = ""
        val cursor = if (id_str == "") {
            dbManager.ejectSQLRawQuery("SELECT fav FROM $TableName WHERE $ColumnID=$id_int;")
        } else {
            dbManager.ejectSQLRawQuery("SELECT fav FROM $TableName WHERE $ColumnID='$id_str';")
        }

        val contentValues = ContentValues()
        if (cursor.moveToFirst()) {
            if (cursor.getString(0) == "0") {
                contentValues.put("fav", "1")
                result = "1"
            } else if (cursor.getString(0) == "1") {
                contentValues.put("fav", "0")
                result = "0"
            }

            if (id_str == "") {
                dbManager.update_ForIdInt(TableName, ColumnID, id_int.toLong(), contentValues)
            } else {
                dbManager.update_ForIdStr(TableName, ColumnID, id_str, contentValues)
            }
        }
        cursor.close()
        dbManager.close()
        return result
    }

    @JvmStatic
    fun CorrectOrtogFrases(pcontext: Context) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val dbManager = DBManager(pcontext).open()
            val cursor = dbManager.ejectSQLRawQuery("SELECT id,frase FROM ${DatabaseHelper.T_Frases};")
            val contentValues = ContentValues()
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    var temp = cursor.getString(1)
                    temp = temp.replace(" echos ", " hechos ")
                    temp = temp.replace("enanmórate", "enamórate")
                    contentValues.put(DatabaseHelper.C_frases_frase, temp)
                    dbManager.update_ForIdStr(DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, cursor.getString(1), contentValues)
                    contentValues.clear()
                    cursor.moveToNext()
                }
            }
            cursor.close()
            dbManager.close()
        }
    }

    @JvmStatic
    fun insertNewFrase(pcontext: Context, textFrase: String, autor: String, fuente: String, inbuilt: String): Long {
        val dbManager = DBManager(pcontext).open()
        val contentValues = ContentValues()
        contentValues.put(DatabaseHelper.C_frases_frase, textFrase.trim())
        contentValues.put(DatabaseHelper.C_frases_autor, autor.trim())
        contentValues.put(DatabaseHelper.C_frases_fuente, fuente.trim())
        contentValues.put(DatabaseHelper.CC_favorito, "0")
        contentValues.put(DatabaseHelper.CC_nota, "")
        contentValues.put(DatabaseHelper.C_frases_in_built, inbuilt)
        contentValues.put(DatabaseHelper.C_frases_shared, "0")
        val result = dbManager.insert(DatabaseHelper.T_Frases, contentValues)
        dbManager.close()
        return result
    }

    @JvmStatic
    fun insertNewApunte(context: Context, title: String, apunte: String): Long {
        val dbManager = DBManager(context).open()
        val contentValues = ContentValues()
        contentValues.put(DatabaseHelper.C_apunte_title, title.trim())
        contentValues.put(DatabaseHelper.CC_nota, apunte.trim())
        val result = dbManager.insert(DatabaseHelper.T_Apuntes, contentValues)
        dbManager.close()
        return result
    }

    @JvmStatic
    fun updateApunte(context: Context, title: String, apunte: String): Boolean {
        var result = false
        val dbManager = DBManager(context).open()
        val query = "UPDATE ${DatabaseHelper.T_Apuntes} SET ${DatabaseHelper.CC_nota}='$apunte' WHERE ${DatabaseHelper.C_apunte_title}='$title';"
        try {
            dbManager.ejectSQLCommand(query)
            result = true
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        dbManager.close()
        return result
    }

    @JvmStatic
    fun updateNota(context: Context, tableName: String, columnID: String, valorID: String, nota: String): Boolean {
        var result = false
        val dbManager = DBManager(context).open()
        val query = "UPDATE $tableName SET ${DatabaseHelper.CC_nota}='$nota' WHERE $columnID='$valorID';"
        try {
            dbManager.ejectSQLCommand(query)
            result = true
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        dbManager.close()
        return result
    }

    @JvmStatic
    fun loadConferenciaList(pcontext: Context): List<String> {
        val result: MutableList<String> = LinkedList()
        val dbManager = DBManager(pcontext).open()
        val cursor = dbManager.ejectSQLRawQuery("SELECT ${DatabaseHelper.C_conf_title} FROM ${DatabaseHelper.T_Conf}")
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                result.add(cursor.getString(0).replace(".txt", ""))
                cursor.moveToNext()
            }
        }
        cursor.close()
        dbManager.close()
        return result
    }

    @JvmStatic
    fun LoadVideoList(context: Context, typeOfVideo: String, listaUrl: MutableList<String>, lista: MutableList<String>) {
        lista.clear()
        listaUrl.clear()
        val dbManager = DBManager(context).open()
        val cursor = dbManager.ejectSQLRawQuery("SELECT * FROM ${DatabaseHelper.T_Videos} WHERE ${DatabaseHelper.C_videos_type}='$typeOfVideo';")
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                lista.add(cursor.getString(1))
                listaUrl.add(cursor.getString(2))
                cursor.moveToNext()
            }
        }
        cursor.close()
        dbManager.close()
    }
}
