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
