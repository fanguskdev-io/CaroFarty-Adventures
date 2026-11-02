package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.EndCause
import com.fanguskdev.carofarty.game.Scenario

/** Every word on the screens, in Spanish and in capitals, as the pixel font draws them. */
internal object Texts {
    const val TAP_TO_START = "TOCA PARA EMPEZAR"
    const val HOLD_HINT = "MANTÉN EL DEDO PARA SALTAR MÁS"
    const val POINTS = "PUNTOS"
    const val PAUSE = "PAUSA"
    const val TAP_TO_RESUME = "TOCA PARA SEGUIR"
    const val GAME_OVER = "FIN DE LA PARTIDA"
    const val NEW_RECORD = "¡NUEVO RÉCORD!"
    const val TAP_TO_RETRY = "TOCA PARA JUGAR OTRA VEZ"
    const val DISTANCE = "DISTANCIA"
    const val MAX_POINTS = "PUNTOS MÁXIMOS"
    const val RECORD = "RÉCORD"
    const val AGAIN = "OTRA VEZ"
    const val HOME = "INICIO"
    const val RECORDS_TITLE = "RÉCORDS"
    const val NO_RECORDS = "TODAVÍA NO HAS JUGADO AQUÍ"
    const val NEXT = "SIGUIENTE"
    const val PREVIOUS = "ATRÁS"
    const val PLAY = "¡A JUGAR!"

    const val HOW_TO_TITLE = "CÓMO SE JUEGA"

    /** The two lines of each page of how to play. */
    fun howTo(page: Int): List<String> = when (page) {
        0 -> listOf("TOCA PARA SALTAR", "MANTÉN EL DEDO PARA SALTAR MÁS ALTO Y MÁS LEJOS")
        1 -> listOf("RESCATA PERRITOS Y GATITOS", "CADA UNO SUMA 1 PUNTO")
        2 -> listOf("EVITA EL HUEVO FRITO Y EL PESCADO", "EL HUEVO RESTA 2 PUNTOS Y EL PESCADO 3")
        else -> listOf("LA PARTIDA SE ACABA SI CAES A UN BARRANCO", "O SI TE QUEDAS SIN PUNTOS")
    }

    /** Where a run got in among the best of its scenario, when it is not the first. */
    fun place(place: Int, of: Int): String = "PUESTO $place DE TUS $of MEJORES"

    fun cause(cause: EndCause?): String = when (cause) {
        EndCause.Fall -> "TE CAÍSTE POR UN BARRANCO"
        EndCause.NoPoints -> "TE QUEDASTE SIN PUNTOS"
        null -> ""
    }

    fun scenario(scenario: Scenario): String = when (scenario) {
        Scenario.Bosque -> "BOSQUE"
        Scenario.Nieve -> "NIEVE"
        Scenario.Desierto -> "DESIERTO"
    }
}
