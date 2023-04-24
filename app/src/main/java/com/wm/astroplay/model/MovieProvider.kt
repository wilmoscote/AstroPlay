package com.wm.astroplay.model

import com.google.firebase.Timestamp

object MovieProvider {
    fun getMovies(): List<Movie> {
        return listOf(

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
            "Suspenso",
            "Guerra"
        )
    }
}
