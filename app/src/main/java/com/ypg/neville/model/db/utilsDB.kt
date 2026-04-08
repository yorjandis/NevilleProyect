package com.ypg.neville.model.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.preference.PreferenceManager
import com.ypg.neville.model.db.room.ConfEntity
import com.ypg.neville.model.db.room.FraseEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.NotaEntity
import com.ypg.neville.model.frases.FrasesAssetParser
import com.ypg.neville.model.frases.FrasesAssetSyncManager
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.model.utils.GetFromRepo
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.Executors

// Clase que se encarga de realizar operaciones específicas y de ayuda a la BD
object utilsDB {

    @Volatile
    private var legacyMigrationChecked = false

    private fun db(context: Context) = NevilleRoomDatabase.getInstance(context)

    @JvmStatic
    fun hasLegacyDatabase(context: Context): Boolean {
        return context.getDatabasePath(DatabaseHelper.DB_NAME).exists()
    }

    /**
     * Migra datos desde la BD legacy (neville.db) al esquema Room actual.
     * El fichero legacy se elimina al finalizar.
     */
    @JvmStatic
    fun migrateLegacyDatabaseIfNeeded(context: Context): Boolean {
        synchronized(this) {
            if (legacyMigrationChecked) return false
            legacyMigrationChecked = true
        }

        val legacyFile = context.getDatabasePath(DatabaseHelper.DB_NAME)
        if (!legacyFile.exists()) return false

        var migratedAny = false
        val room = db(context)
        val legacyDb = SQLiteDatabase.openDatabase(legacyFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

        try {
            if (tableExists(legacyDb, DatabaseHelper.T_Frases) && room.fraseDao().count() == 0) {
                room.fraseDao().insertAll(readLegacyFrases(legacyDb))
                migratedAny = true
            }

            if (tableExists(legacyDb, DatabaseHelper.T_Conf) && room.confDao().count() == 0) {
                room.confDao().insertAll(readLegacyConf(legacyDb))
                migratedAny = true
            }

            // legacy notas(title, nota) -> Room notas(titulo, nota, fecha...)
            if (tableExists(legacyDb, DatabaseHelper.T_Apuntes)) {
                val legacyNotas = readLegacyApuntes(legacyDb)
                val now = System.currentTimeMillis()
                for ((title, note) in legacyNotas) {
                    if (room.notaDao().getByTitulo(title) == null) {
                        room.notaDao().insert(
                            NotaEntity(
                                titulo = title,
                                nota = note,
                                fechaCreacion = now,
                                fechaModificacion = now
                            )
                        )
                        migratedAny = true
                    }
                }
            }
        } finally {
            legacyDb.close()
        }

        context.deleteDatabase(DatabaseHelper.DB_NAME)
        return migratedAny
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean {
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun readLegacyFrases(db: SQLiteDatabase): List<FraseEntity> {
        val list = mutableListOf<FraseEntity>()
        db.rawQuery("SELECT id, frase, autor, fuente, fav, nota, inbuild, shared FROM frases", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    FraseEntity(
                        id = c.getLong(0),
                        frase = c.getString(1) ?: "",
                        autor = c.getString(2) ?: "",
                        fuente = c.getString(3) ?: "",
                        isfav = c.getString(4) ?: "0",
                        personal = if ((c.getString(6) ?: "0") == "0") "1" else "0",
                        fav = c.getString(4) ?: "0",
                        nota = c.getString(5) ?: "",
                        inbuild = c.getString(6) ?: "0",
                        categoria = FrasesAssetParser.CATEGORIA_AUTOR,
                        assetKey = "",
                        assetHash = "",
                        shared = c.getString(7) ?: "0"
                    )
                )
            }
        }
        return list
    }

    private fun readLegacyConf(db: SQLiteDatabase): List<ConfEntity> {
        val list = mutableListOf<ConfEntity>()
        db.rawQuery("SELECT id, title, link, fav, nota, shared FROM conf", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ConfEntity(
                        id = c.getLong(0),
                        title = c.getString(1) ?: "",
                        link = c.getString(2) ?: "",
                        fav = c.getString(3) ?: "0",
                        nota = c.getString(4) ?: "",
                        shared = c.getString(5) ?: "0"
                    )
                )
            }
        }
        return list
    }

    private fun readLegacyApuntes(db: SQLiteDatabase): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        db.rawQuery("SELECT title, nota FROM notas", null).use { c ->
            while (c.moveToNext()) {
                list.add((c.getString(0) ?: "") to (c.getString(1) ?: ""))
            }
        }
        return list
    }

    /**
     * Pasa las frases (inbuilt) de xml a la tabla frases (Room)
     */
    @Suppress("unused")
    @JvmStatic
    fun yor_populateFraseTable(context: Context) {
        FrasesAssetSyncManager.forceSync(context, db(context))
    }

    /**
     * Pasa conferencias inbuilt a tabla conf (Room)
     */
    @JvmStatic
    @Throws(IOException::class)
    fun yor_populateConfTable(context: Context) {
        val dao = db(context).confDao()
        dao.clearAll()

        val fileList = GetFromRepo.getConfListFromAssets(context)
        val confs = mutableListOf<ConfEntity>()

        if (fileList != null) {
            for (file in fileList) {
                val titleNormalized = file
                    .removePrefix("conf_")
                    .replace(".txt", "")
                confs.add(
                    ConfEntity(
                        title = titleNormalized,
                        link = file,
                        fav = "0",
                        nota = "",
                        shared = "0"
                    )
                )
            }
        }

        dao.insertAll(confs)
    }

    /**
     * Normaliza conferencias existentes para el nuevo esquema de nombres:
     * - title: sin prefijo "conf_" y sin ".txt"
     * - link: con prefijo "conf_" y extensión ".txt"
     *
     * Si encuentra duplicados por título normalizado, los fusiona preservando
     * favoritos/notas y se queda con un único registro.
     */
    @JvmStatic
    fun migrateConfNamingIfNeeded(context: Context): Boolean {
        val dao = db(context).confDao()
        val current = dao.getAll()
        if (current.isEmpty()) return false

        var changed = false
        val mergedByTitle = linkedMapOf<String, ConfEntity>()

        for (item in current) {
            val normalizedTitle = normalizeConfTitle(item.title)
            val normalizedLink = normalizeConfLink(item.link, normalizedTitle)

            val normalizedItem = item.copy(
                title = normalizedTitle,
                link = normalizedLink
            )

            if (normalizedItem != item) {
                changed = true
            }

            val existing = mergedByTitle[normalizedTitle]
            if (existing == null) {
                mergedByTitle[normalizedTitle] = normalizedItem
            } else {
                changed = true
                mergedByTitle[normalizedTitle] = mergeConf(existing, normalizedItem)
            }
        }

        if (!changed && mergedByTitle.size == current.size) return false

        // Reescribir la tabla deja el estado consistente para lecturas por título.
        dao.clearAll()
        dao.insertAll(
            mergedByTitle.values.map { it.copy(id = 0) }
        )
        return true
    }

    private fun normalizeConfTitle(raw: String): String {
        return raw
            .trim()
            .removePrefix("conf_")
            .removeSuffix(".txt")
    }

    private fun normalizeConfLink(raw: String, normalizedTitle: String): String {
        val source = raw.trim().ifEmpty { normalizedTitle }
        val baseName = source
            .removePrefix("autores/neville/conf/")
            .removeSuffix(".txt")

        val prefixed = if (baseName.startsWith("conf_")) baseName else "conf_$baseName"
        return "$prefixed.txt"
    }

    private fun mergeConf(a: ConfEntity, b: ConfEntity): ConfEntity {
        val nota = when {
            a.nota.isNotBlank() && b.nota.isBlank() -> a.nota
            b.nota.isNotBlank() && a.nota.isBlank() -> b.nota
            b.nota.length > a.nota.length -> b.nota
            else -> a.nota
        }

        return a.copy(
            // Mantener título y link normalizados
            title = a.title,
            link = if (a.link.length >= b.link.length) a.link else b.link,
            fav = if (a.fav == "1" || b.fav == "1") "1" else "0",
            nota = nota,
            shared = if (a.shared == "1" || b.shared == "1") "1" else "0"
        )
    }

    /**
     * Restaura tablas semilla cuando están vacías
     */
    @JvmStatic
    fun RestoreDBInfo(context: Context): Boolean {
        migrateLegacyDatabaseIfNeeded(context)

        var result = false
        val room = db(context)

        if (FrasesAssetSyncManager.syncIfNeeded(context, room)) {
            result = true
        }

        if (room.confDao().count() == 0) {
            try {
                yor_populateConfTable(context)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            result = true
        }

        if (migrateConfNamingIfNeeded(context)) {
            result = true
        }

        return result
    }

    @JvmStatic
    fun readFavState(context: Context, tableName: String, columnID: String, id: String): String {
        val room = db(context)
        return when (tableName) {
            DatabaseHelper.T_Frases -> {
                when (columnID) {
                    DatabaseHelper.C_frases_frase -> room.fraseDao().getByFrase(id)?.favState() ?: ""
                    else -> ""
                }
            }
            DatabaseHelper.T_Conf -> room.confDao().getByTitle(id)?.fav ?: ""
            else -> ""
        }
    }

    @JvmStatic
    fun UpdateFavorito(context: Context, tableName: String, columnID: String, id_str: String, id_int: Int): String {
        val room = db(context)
        return when (tableName) {
            DatabaseHelper.T_Frases -> {
                if (id_str.isEmpty() && columnID == DatabaseHelper.CC_id) {
                    val item = room.fraseDao().getById(id_int.toLong()) ?: return ""
                    val target = if (item.favState() == "1") "0" else "1"
                    room.fraseDao().updateFavById(item.id, target)
                    target
                } else {
                    val item = room.fraseDao().getByFrase(id_str) ?: return ""
                    val target = if (item.favState() == "1") "0" else "1"
                    room.fraseDao().updateFavByFrase(item.frase, target)
                    target
                }
            }
            DatabaseHelper.T_Conf -> {
                val item = room.confDao().getByTitle(id_str) ?: return ""
                val target = if (item.fav == "1") "0" else "1"
                room.confDao().updateFavByTitle(item.title, target)
                target
            }
            else -> ""
        }
    }

    @JvmStatic
    fun CorrectOrtogFrases(pcontext: Context) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val dao = db(pcontext).fraseDao()
            val items = dao.getAll()
            for (item in items) {
                var temp = item.frase
                temp = temp.replace(" echos ", " hechos ")
                temp = temp.replace("enanmórate", "enamórate")
                if (temp != item.frase) {
                    dao.updateFraseTextById(item.id, temp)
                }
            }
        }
    }

    @JvmStatic
    fun insertNewFrase(pcontext: Context, textFrase: String, autor: String, fuente: String, inbuilt: String): Long {
        val personal = if (inbuilt == "0") "1" else "0"
        val categoria = if (autor.trim().equals("salud", ignoreCase = true)) {
            FrasesAssetParser.CATEGORIA_SALUD
        } else {
            FrasesAssetParser.CATEGORIA_AUTOR
        }
        return db(pcontext).fraseDao().insert(
            FraseEntity(
                frase = textFrase.trim(),
                autor = autor.trim(),
                fuente = fuente.trim(),
                isfav = "0",
                personal = personal,
                fav = "0",
                nota = "",
                inbuild = inbuilt,
                categoria = categoria,
                assetKey = "",
                assetHash = "",
                shared = "0"
            )
        )
    }

    @JvmStatic
    fun insertNewApunte(context: Context, title: String, apunte: String): Long {
        val now = System.currentTimeMillis()
        return db(context).notaDao().insert(
            NotaEntity(
                titulo = title.trim(),
                nota = apunte.trim(),
                fechaCreacion = now,
                fechaModificacion = now
            )
        )
    }

    @JvmStatic
    fun updateApunte(context: Context, title: String, apunte: String): Boolean {
        val dao = db(context).notaDao()
        val current = dao.getByTitulo(title) ?: return false
        dao.update(
            current.copy(
                nota = apunte,
                fechaModificacion = System.currentTimeMillis()
            )
        )
        return true
    }

    @JvmStatic
    fun updateNota(context: Context, tableName: String, columnID: String, valorID: String, nota: String): Boolean {
        val room = db(context)
        when (tableName) {
            DatabaseHelper.T_Frases -> if (columnID == DatabaseHelper.C_frases_frase) room.fraseDao().updateNotaByFrase(valorID, nota)
            DatabaseHelper.T_Conf -> room.confDao().updateNotaByTitle(valorID, nota)
            else -> return false
        }
        return true
    }

    @JvmStatic
    fun loadConferenciaList(pcontext: Context): List<String> {
        return db(pcontext).confDao().getAll().map { it.title.replace(".txt", "") }
    }

    // Métodos de soporte para reemplazar lógica SQL legacy en vistas
    @JvmStatic
    fun getFraseNota(context: Context, frase: String): String {
        return db(context).fraseDao().getByFrase(frase)?.nota ?: ""
    }

    @JvmStatic
    fun getConfNota(context: Context, title: String): String {
        return db(context).confDao().getByTitle(title)?.nota ?: ""
    }

    @JvmStatic
    fun getFraseById(context: Context, id: Long): FraseEntity? {
        return db(context).fraseDao().getById(id)
    }

    @JvmStatic
    fun getApunteByTitle(context: Context, title: String): NotaEntity? {
        return db(context).notaDao().getByTitulo(title)
    }

    @Suppress("unused")
    @JvmStatic
    fun deleteFraseByText(context: Context, frase: String) {
        db(context).fraseDao().deleteByFrase(frase)
    }

    @Suppress("unused")
    @JvmStatic
    fun deleteApunteByTitle(context: Context, title: String) {
        db(context).notaDao().deleteByTitulo(title)
    }

    @JvmStatic
    fun getListadoTitles(context: Context, filtro: String): List<String> {
        val room = db(context)
        val result = when (filtro) {
            "Todas las frases" -> room.fraseDao().getAll().map { it.frase }
            "Frases inbuilt" -> room.fraseDao().getInbuilt().map { it.frase }
            "Frases favoritas" -> room.fraseDao().getFavoritas().map { it.frase }
            "Frases inbuilt favoritas" -> room.fraseDao().getInbuiltFavoritas().map { it.frase }
            "Frases inbuilt con notas" -> room.fraseDao().getInbuiltConNotas().map { it.frase }
            "Frases personales" -> room.fraseDao().getPersonales().map { it.frase }
            "Frases personales favoritas" -> room.fraseDao().getPersonalesFavoritas().map { it.frase }
            "Frases personales con notas" -> room.fraseDao().getPersonalesConNotas().map { it.frase }
            "Todas las conf" -> room.confDao().getAll().map { it.title }
            "Conferencias favoritas" -> room.confDao().getFavoritas().map { it.title }
            "Conferencias con notas" -> room.confDao().getConNotas().map { it.title }
            "Apuntes" -> room.notaDao().getAll().map { it.titulo }
            else -> emptyList()
        }

        return LinkedList(result)
    }

    @JvmStatic
    fun getRandomFrase(context: Context, onlyFav: Boolean): FraseEntity? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val allowNeville = prefs.getBoolean("home_filter_author_neville", true)
        val allowJoe = prefs.getBoolean("home_filter_author_joe", true)
        val allowGregg = prefs.getBoolean("home_filter_author_gregg", true)
        val allowBruce = prefs.getBoolean("home_filter_author_bruce", true)
        val includeAutores = allowNeville || allowJoe || allowGregg || allowBruce
        val filter = FraseHomeFilter(
            includeAutores = includeAutores,
            includeOtros = prefs.getBoolean("home_filter_otros", true),
            includeSalud = prefs.getBoolean("home_filter_salud", true),
            includeNeville = allowNeville,
            includeJoe = allowJoe,
            includeGregg = allowGregg,
            includeBruce = allowBruce
        )
        val items = db(context).fraseDao().getForHome(
            onlyFav = if (onlyFav) 1 else 0,
            includeAutores = if (filter.includeAutores) 1 else 0,
            includeOtros = if (filter.includeOtros) 1 else 0,
            includeSalud = if (filter.includeSalud) 1 else 0
        ).filter { frase ->
            val isPremiumActive = SubscriptionManager.hasActiveSubscription(context)
            if (!isPremiumActive && frase.personalState() != "1") {
                val mustBlockBySubscription = when (frase.categoria.uppercase()) {
                    FrasesAssetParser.CATEGORIA_SALUD, FrasesAssetParser.CATEGORIA_OTROS -> true
                    FrasesAssetParser.CATEGORIA_AUTOR -> !frase.autor.contains("neville", ignoreCase = true)
                    else -> false
                }
                if (mustBlockBySubscription) return@filter false
            }
            if (frase.categoria != FrasesAssetParser.CATEGORIA_AUTOR) return@filter true
            when (frase.autor.trim().lowercase()) {
                "neville goddard" -> filter.includeNeville
                "joe dispenza" -> filter.includeJoe
                "gregg braden" -> filter.includeGregg
                "bruce lipton" -> filter.includeBruce
                else -> false
            }
        }
        if (items.isEmpty()) return null
        return items.random()
    }

    @JvmStatic
    fun getRandomFraseByAutor(context: Context, autor: String): FraseEntity? {
        val items = db(context).fraseDao().getByAutor(autor).filter { it.personalState() != "1" }
        if (items.isEmpty()) return null
        return items.random()
    }

    @JvmStatic
    fun forceRefreshFrasesFromAssets(context: Context): Boolean {
        return FrasesAssetSyncManager.forceSync(context, db(context))
    }

    @JvmStatic
    fun getRandomConf(context: Context, onlyFav: Boolean): ConfEntity? {
        val items = if (onlyFav) db(context).confDao().getFavoritas() else db(context).confDao().getAll()
        if (items.isEmpty()) return null
        return items.random()
    }

    @JvmStatic
    fun getAllConfTitles(context: Context): List<String> = db(context).confDao().getAll().map { it.title }

    @JvmStatic
    fun getAllFrases(context: Context): List<FraseEntity> = db(context).fraseDao().getAll()

    data class FraseHomeFilter(
        val includeAutores: Boolean,
        val includeOtros: Boolean,
        val includeSalud: Boolean,
        val includeNeville: Boolean,
        val includeJoe: Boolean,
        val includeGregg: Boolean,
        val includeBruce: Boolean
    )
}
