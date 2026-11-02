package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario

/**
 * The instruments of one scenario, made from their recipes (the slow part of starting the music: tens of milliseconds).
 * Build it once per scenario and give it to every [MusicStream] of that scenario, so a new run does not make it again.
 */
class MusicBank internal constructor(internal val style: MusicStyle) {
    /** The instruments of [scenario]. */
    constructor(scenario: Scenario) : this(styleOf(scenario))

    internal val instruments: Array<Instrument>
    private val recipes: List<InstrumentRecipe>

    init {
        val all = ArrayList<InstrumentRecipe>()
        for (recipe in style.roles.values + style.arrangement.instruments) {
            if (all.none { it === recipe }) all.add(recipe)
        }
        recipes = all
        instruments = Array(all.size) { Instrument(all[it]) }
    }

    /** The index of an instrument in [instruments]. */
    internal fun indexOf(recipe: InstrumentRecipe): Int {
        val i = recipes.indexOfFirst { it === recipe }
        require(i >= 0) { "${recipe.name} is not in the bank of ${style.name}" }
        return i
    }

    /** The index of the instrument of a role of the style ("bass", "pad", "c2", "c3", "c3b"…). */
    internal fun role(name: String): Int = indexOf(style.roles[name] ?: throw IllegalArgumentException("${style.name} has no $name"))
}
