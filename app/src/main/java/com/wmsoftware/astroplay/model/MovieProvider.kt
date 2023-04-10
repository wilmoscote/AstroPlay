package com.wmsoftware.astroplay.model

import com.google.firebase.Timestamp

object MovieProvider {
    fun getMovies(): List<Movie> {
        return listOf(
            Movie(
                title = "Pantera Negra: Wakanda por siempre",
                originalTitle = "Black Panther: Wakanda Forever",
                year = "2022",
                director = "Ryan Coogler",
                actors = listOf("Letitia Wright", "Lupita Nyong'o", "Danai Gurira"),
                genre = listOf("Acción", "Aventura", "Drama"),
                runtime = "2h 41min",
                plot = "El pueblo de Wakanda lucha para proteger su hogar de las potencias mundiales interventoras mientras lloran la muerte de su rey T'Challa.",
                poster = "https://firebasestorage.googleapis.com/v0/b/astroplay.appspot.com/o/covers%2Fblackpantherwakandaforever.jpg?alt=media&token=d28cba58-2b3b-4d0d-b090-38595461a112",
                imdbRating = "6.7",
                appRating = "4.5",
                views = 1,
                language = "Español",
                ageRating = "B",
                "https://firebasestorage.googleapis.com/v0/b/astroplay.appspot.com/o/movies%2Fblackpantherwakandaforever.mp4?alt=media&token=a814a37a-a87d-4b10-a582-d3750254827c",
                createdAt = Timestamp.now(),
            ),
        )
    }

    fun getQuotes(): List<String> {
        return listOf(
            "¿Qué deseas ver hoy?",
            "¡Encuentra tu próxima aventura!",
            "¿Listo para una nueva historia?",
            "¡Sumérgete en un mundo de cine!",
            "¡Ponte cómodo y disfruta!",
            "Explora y disfruta.",
            "¡Tu película favorita te espera!",
            "¡Escoge, reproduce y disfruta!",
            "¡Relájate con una buena película!",
            "¡Hay tanto por descubrir!",
            "¡Que comience la función!"
        )
    }


    fun getCategories(): List<String>{
        return  listOf(
            "Acción",
            "Aventura",
            "Animación",
            "Biografía",
            "Comedia",
            "Crimen",
            "Documental",
            "Drama",
            "Familia",
            "Fantasía",
            "Cine negro",
            "Historia",
            "Terror",
            "Musical",
            "Misterio",
            "Romance",
            "Ciencia ficción",
            "Deportes",
            "Suspense",
            "Guerra"
        )
    }
}
