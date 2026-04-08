package com.ypg.neville.model.utils

object NewsContent {

    private const val INTRO = "Bienvenidos a Neville Para Todos!!!"
    private const val VERSION_TITLE = "La version actual cuenta con las siguientes características:"
    private const val CLOSING = "En vosotros mora el espíritu del Dios viviente!"

    // Añade nuevas líneas aquí para actualizar rápidamente la sección de novedades.
    private val highlights = listOf(
        "Nueva apariencia visual personalizable.",
        "Contenido exclusivo de varios autores reconocidos: Joe Dispenza, Bruce Lipton, Gregg Braden.",
        "Diario Personal con acceso protegido. Registre su progreso y sus experiencias.",
        "Ahora las notas pueden protegerse con biometría.",
        "Metas y transformación personal: Adopta hábitos saludables respaldados por la neurociencia y sigue programas para empoderarte",
        "Lienzo creativo: Diseña imágines con tus frases favoritas y compártelas en las redes sociales.",
        "Enciclopedia: Contenidos selectos sobre varias materias relacionadas con las enseñanzas de neville y los demás autores.",
        "Evidencia Científica: Resumen de investigaciones que apoyan las enseñanzas de esta obra, debidamente acotados y referenciados.",
        "Se ha añadido 171 nuevas conferencias de neville, ahora suman 472",
        "Compendio extendido de frases de varios autores, 1071 frases para empoderarte y ofrecerte valiosos conocimientos.",
        "32 citas de conferencias",
        "36 preguntas y respuestas de neville",
        "Posibilidad de adicionar nuestras propias frases y de compartirlas",
        "Personalización del tema y del contenido"
    )

    @JvmStatic
    fun buildNewsText(): String {
        val body = highlights.joinToString(separator = "\n\n") { "☘️ $it" }
        return "$INTRO\n\n$VERSION_TITLE\n\n$body\n\n$CLOSING"
    }
}

