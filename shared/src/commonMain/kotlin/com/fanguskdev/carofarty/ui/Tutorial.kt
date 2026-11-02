package com.fanguskdev.carofarty.ui

import com.fanguskdev.carofarty.game.GameConfig
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.Ground
import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.JumpLab
import com.fanguskdev.carofarty.game.Piece
import com.fanguskdev.carofarty.game.PieceSource
import com.fanguskdev.carofarty.game.Pilot
import com.fanguskdev.carofarty.game.RunEvent
import com.fanguskdev.carofarty.game.RunPhase
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.game.TILE
import com.fanguskdev.carofarty.ui.render.CaroLook

/** A number that floats up over Caro from the height where she picked something up, in how to play: «+1», «-2». */
internal class Floating(val text: String, val good: Boolean, val y: Float) {
    var age = 0f
}

/**
 * A scene of how to play (GDD-17, UI-12, Fangusk after the beta, GameMaster's storyboard): a small run that plays by
 * itself, with Caro's real jump and pickups, to show one thing. Scene 0, jumping: a short gap and a wider one, which the
 * pilot clears with a short and a longer hold; 1, the perritos and gatitos that add a point; 2, the egg and the fish, which
 * she picks up, to see what they take away; 3, the end: a gap with no ground after it. Each starts again when it has
 * shown its thing. [newLook] makes Caro's look.
 */
internal class TutorialScene(
    val index: Int,
    private val config: GameConfig,
    val scenario: Scenario,
    private val newLook: () -> CaroLook,
) {
    var run = newRun()
        private set
    var look = newLook()
        private set
    private var pilot: Pilot? = pilotFor(run)
    private var overFor = 0f

    /** The numbers floating up from what she picked up. */
    val floating = ArrayList<Floating>()

    /** Whether the finger is down, to show it on the scene; and for how long a tap still shows, so a short one is seen. */
    var held = false
        private set
    var touching = 0f
        private set

    /** For how long the finger has been down, to tell a tap from a hold. */
    var heldFor = 0f
        private set

    /** How many rows of ground show under her feet: more where there is a gap to see. */
    val groundShown: Int get() = if (index == JUMPING || index == ENDING) GROUND_WITH_GAP else GROUND

    fun step() {
        val pilot = pilot
        pilot?.decide()
        held = pilot?.held == true || pilot?.pressed == true
        touching = if (pilot?.pressed == true) TOUCH_SECONDS else maxOf(0f, touching - JumpLab.STEP)
        heldFor = if (held) heldFor + JumpLab.STEP else 0f
        run.step(JumpLab.STEP, pilot?.pressed ?: false, held)
        for (event in run.events) {
            if (event is RunEvent.Picked) {
                val text = if (event.points > 0) "+${event.points}" else "${event.points}"
                floating += Floating(text, event.points > 0, run.caro.top)
            }
        }
        run.events.clear()
        look.step(JumpLab.STEP, run, newRecord = false)
        for (i in floating.indices.reversed()) {
            floating[i].age += JumpLab.STEP
            if (floating[i].age > FLOAT_SECONDS) floating.removeAt(i)
        }
        if (run.phase == RunPhase.Over) overFor += JumpLab.STEP
        if (run.caro.x > LOOP_AT[index] || overFor > OVER_SECONDS) restart()
    }

    private fun restart() {
        run = newRun()
        look = newLook()
        pilot = pilotFor(run)
        overFor = 0f
        floating.clear()
        held = false
        touching = 0f
        heldFor = 0f
    }

    private fun pilotFor(run: GameRun): Pilot? = if (index == JUMPING) Pilot(run) else null

    private fun newRun(): GameRun {
        val piece = when (index) {
            JUMPING -> Piece(
                listOf(Ground(-SIDE, 21 * TILE, TOP), Ground(23 * TILE, 44 * TILE, TOP), Ground(48 * TILE, FAR, TOP)),
                emptyList(),
                FAR,
            )
            RESCUING -> Piece(listOf(Ground(-SIDE, FAR, TOP)), animals(), FAR)
            AVOIDING -> Piece(listOf(Ground(-SIDE, FAR, TOP)), listOf(item(ItemKind.Huevo, 16), item(ItemKind.Pescado, 30)), FAR)
            else -> Piece(listOf(Ground(-SIDE, 14 * TILE, TOP)), emptyList(), FAR)
        }
        val pieces = object : PieceSource {
            private var given = false

            override fun nextOrNull(): Piece? {
                if (given) return null
                given = true
                return piece
            }
        }
        return GameRun(config, scenario, seed = index.toLong(), pieces = pieces).also {
            it.step(JumpLab.STEP, pressed = true, held = false)
        }
    }

    private fun animals(): List<Item> = listOf(
        item(ItemKind.Gatito, 12).also { it.coat = 0 },
        item(ItemKind.Perrito, 16).also { it.breed = 8 },
        item(ItemKind.Gatito, 20).also { it.coat = 4 },
        item(ItemKind.Perrito, 24).also { it.breed = 3 },
    )

    private fun item(kind: ItemKind, tile: Int) = Item(kind, tile * TILE, TOP + Item.ABOVE_GROUND)

    companion object {
        const val JUMPING = 0
        const val RESCUING = 1
        const val AVOIDING = 2
        const val ENDING = 3
        const val SCENES = 4

        /** How to play shows them two to a page. */
        const val PAGES = 2
        const val PER_PAGE = 2

        /** A finger down longer than this is holding the jump, not tapping. */
        const val TAP_SECONDS = 0.1f

        /** The ground's top in the scenes, two tiles over the bottom, like the start of a run. */
        const val TOP = 2 * TILE

        /** The rows of ground under her feet in the scenes (GameMaster's mockup): 12, and 20 where there are gaps. */
        private const val GROUND = 12
        private const val GROUND_WITH_GAP = 20
        private const val TOUCH_SECONDS = 0.2f
        private const val SIDE = 32 * TILE
        private const val FAR = 400 * TILE

        /** Where each scene starts again, in world pixels; the last one, a moment after she fell. */
        private val LOOP_AT = doubleArrayOf(56.0 * TILE, 30.0 * TILE, 38.0 * TILE, Double.MAX_VALUE)
        private const val OVER_SECONDS = 1.2f
        const val FLOAT_SECONDS = 0.7f
    }
}
