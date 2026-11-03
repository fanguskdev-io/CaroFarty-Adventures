package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.EndCause
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.Scenario

/** Every word on the screens, in Spanish and in capitals, as the pixel font draws them. */
internal object Texts {
    const val HOLD_HINT = "MANTÉN"
    const val POINTS = "PUNTOS"
    const val PAUSE = "PAUSA"
    const val RESUME = "CONTINUAR"
    const val RESTART = "REINICIAR"
    const val TAP_TO_START = "TOCA PARA EMPEZAR"

    /** The subtitle of the title screen, which Fangusk chose. */
    const val SUBTITLE = "SALVANDO EL MUNDO, PATITA A PATITA"
    const val NEW_RECORD = "¡NUEVO RÉCORD!"
    const val DISTANCE = "DISTANCIA"
    const val MAX_POINTS = "PUNTOS MÁXIMOS"
    const val RECORD = "RÉCORD"
    const val AGAIN = "OTRA VEZ"
    const val HOME = "INICIO"
    const val RECORDS_TITLE = "RÉCORDS"
    const val NO_RECORDS = "AÚN NO HAS JUGADO AQUÍ"
    const val METERS = "METROS"
    const val DAY = "FECHA"
    const val NEXT = "SIGUIENTE"
    const val PLAY = "¡A JUGAR!"
    const val UNDERSTOOD = "¡ENTENDIDO!"
    const val QUIT_QUESTION = "¿SALIR DEL JUEGO?"
    const val YES = "SÍ"
    const val NO = "NO"

    const val HOW_TO_TITLE = "CÓMO SE JUEGA"

    /** The two lines under each scene of how to play (GameMaster's mockup). */
    fun howTo(scene: Int): List<String> = when (scene) {
        0 -> listOf("TOCA PARA SALTAR", "MANTÉN PARA SALTAR MÁS")
        1 -> listOf("RESCATA PERRITOS", "Y GATITOS: +1")
        2 -> listOf("EVITA EL HUEVO: -3", "Y EL PESCADO: -5")
        else -> listOf("SE ACABA SI CAES", "O SI LLEGAS A 0 PUNTOS")
    }

    /** Where a run got in among the best of its scenario, when it is not the first. */
    fun place(place: Int, of: Int): String = "PUESTO $place DE TUS $of MEJORES"

    /**
     * The title of the end, with a joke by how she lost, which Fangusk chose: into the water of the Bosque and the Nieve or
     * the canyon of the Desierto, or out of points by the smell of the food or, if a bird took the last ones, its peck
     * (GameMaster's touch). [by] is what took her points last.
     */
    fun endTitle(cause: EndCause?, scenario: Scenario, by: ItemKind?): String = when (cause) {
        EndCause.Fall -> if (scenario == Scenario.Desierto) "¡UPS! ¡AL CAÑÓN!" else "¡UPS! ¡AL AGUA!"
        EndCause.NoPoints -> if (by == ItemKind.Pajaro) "¡AY! ¡QUÉ PICOTAZO!" else "¡PUAJ! DEMASIADO MAL OLOR"
        null -> ""
    }

    /** The animals she saved in the run, under the title (Fangusk: «Salvando el mundo, patita a patita»). */
    fun saved(count: Int): String = when (count) {
        0 -> "¡LOS ANIMALITOS TE ESPERAN!"
        1 -> "HAS SALVADO 1 ANIMALITO"
        else -> "HAS SALVADO $count ANIMALITOS"
    }

    fun scenario(scenario: Scenario): String = when (scenario) {
        Scenario.Bosque -> "BOSQUE"
        Scenario.Nieve -> "NIEVE"
        Scenario.Desierto -> "DESIERTO"
    }
}
