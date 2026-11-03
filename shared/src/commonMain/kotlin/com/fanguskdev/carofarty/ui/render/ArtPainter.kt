package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.fanguskdev.carofarty.engine.PixelViewport
import com.fanguskdev.carofarty.game.GameRun
import com.fanguskdev.carofarty.game.Item
import com.fanguskdev.carofarty.game.ItemKind
import com.fanguskdev.carofarty.game.Leap
import com.fanguskdev.carofarty.game.Reaction
import com.fanguskdev.carofarty.game.Scenario
import com.fanguskdev.carofarty.game.Stroll
import com.fanguskdev.carofarty.game.TILE
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The ground of [scenery] in its tiles (TEC-18): the surface on top and the fill under it, alternating two of each so it
 * does not look repeated, and the edges where a ground ends over a gap or rises above its neighbour, so a gap reads at any
 * speed; and the scenery's plant here and there on top. In the low resolution buffer, like paintWorld; [base] is the
 * buffer row of the world's y = 0.
 */
internal fun DrawScope.paintGroundTiles(
    run: GameRun,
    scenery: ScenarioArt,
    cameraLeft: Long,
    cameraBottom: Int,
    width: Int,
    base: Int,
    millis: Long,
) {
    val grounds = run.course.grounds
    for (i in 0 until grounds.size) {
        val ground = grounds[i]
        if (ground.end - cameraLeft <= 0 || ground.start - cameraLeft >= width) continue
        // How high the ground beside it reaches: below that its side is hidden, against the other ground.
        val leftTop = if (i > 0 && grounds[i - 1].end == ground.start) grounds[i - 1].top else 0
        val rightTop = if (i + 1 < grounds.size && grounds[i + 1].start == ground.end) grounds[i + 1].top else 0
        val columns = (ground.end - ground.start + TILE - 1) / TILE
        var column = max(0L, (cameraLeft - ground.start) / TILE - 1).toInt()
        while (column < columns) {
            val x = ground.start + column * TILE
            val left = (x - cameraLeft).toInt()
            if (left >= width) break
            var row = 0
            var top = ground.top
            // Ice or quicksand on the surface of its stretch (PLY-18).
            val patch = ground.patch?.takeIf { x >= it.start && x < it.end }
            val patchTiles = scenery.patch
            while (top > cameraBottom) {
                val bottom = top - TILE
                val frame =
                    when {
                        row == 0 && patch != null && patchTiles != null -> {
                            val animation = when {
                                x == patch.start -> patchTiles.left
                                x + TILE >= patch.end -> patchTiles.right
                                else -> patchTiles.middles[variation(x / TILE, row) % patchTiles.middles.size]
                            }
                            animation.frameAt(millis, loops = true)
                        }
                        column == 0 && bottom >= leftTop -> if (row == 0) scenery.edgeLeft else scenery.fillLeft
                        column == columns - 1 && bottom >= rightTop -> if (row == 0) scenery.edgeRight else scenery.fillRight
                        row == 0 -> scenery.surface[variation(x / TILE, row) % scenery.surface.size]
                        else -> scenery.fill[variation(x / TILE, row) % scenery.fill.size]
                    }
                drawFrame(scenery.tiles, frame, left, base - top)
                row++
                top = bottom
            }
            val plant = scenery.plant
            if (plant != null && column > 0 && column < columns - 1 && variation(x / TILE, PLANT_ROW) % PLANT_EVERY == 0) {
                val time = millis + variation(x / TILE, PLANT_ROW + 1) % OUT_OF_STEP_MILLIS
                drawFrame(scenery.tiles, plant.frameAt(time, loops = true), left, base - ground.top - TILE)
            }
            column++
        }
    }
}

/** The solid blocks in the air (ADR 0007), in the block tile of [scenery]. */
internal fun DrawScope.paintBlockTiles(
    run: GameRun,
    scenery: ScenarioArt,
    cameraLeft: Long,
    width: Int,
    base: Int,
) {
    val blocks = run.course.blocks
    for (i in 0 until blocks.size) {
        val block = blocks[i]
        if (block.start - cameraLeft >= width || block.end - cameraLeft <= 0) continue
        var top = block.top
        while (top > block.bottom) {
            var x = block.start
            while (x < block.end) {
                drawFrame(scenery.tiles, scenery.block, (x - cameraLeft).toInt(), base - top)
                x += TILE
            }
            top -= TILE
        }
    }
}

/**
 * The collectibles with their drawings and animations (GDD-10, ADR 0008): the animals stand on the ground, the perritos
 * walk or stand and face where they go, and the egg and the fish smell. One without its drawing is drawn in grey. [millis]
 * moves the animations, each collectible a little out of step with the rest.
 */
internal fun DrawScope.paintItemSprites(
    run: GameRun,
    art: ItemArt,
    cameraLeft: Long,
    width: Int,
    base: Int,
    millis: Long,
    alpha: Float,
    waterRow: Int,
) {
    val items = run.course.items
    // What flies, where it is between the last two steps, like Caro (Flight).
    val reach = run.course.previousReach + (run.course.reach - run.course.previousReach) * alpha
    for (i in 0 until items.size) {
        val item = items[i]
        if (item.taken) continue
        val flight = item.flight
        if (flight != null && !flight.out(reach)) continue
        val x = ((if (flight == null) item.drawnX(alpha) else item.xAt(reach)).roundToLong() - cameraLeft).toInt()
        if (x - art.anchorX >= width || x - art.anchorX + art.sheet.width <= 0) continue
        val y = base - (if (flight == null) item.y else item.yAt(reach).roundToInt())
        val time = millis + outOfStep(item)
        when (item.kind) {
            ItemKind.Gatito -> {
                val animation = art.gatito
                if (animation == null) {
                    paintGreyItem(item, x, y)
                } else {
                    val sheet = art.coats[item.coat % art.coats.size]
                    drawFrame(sheet, animation.frameAt(time, loops = true), x - art.anchorX, standing(y, sheet, animation))
                }
            }
            ItemKind.Perrito -> {
                if (art.breeds.isEmpty()) {
                    paintGreyItem(item, x, y)
                } else {
                    val breed = art.breeds[item.breed % art.breeds.size]
                    val walk = item.walk
                    val walking = walk?.walking == true
                    val animation = if (walking) breed.walk else breed.still
                    val frame = if (walking) animation.frameAt(time, loops = true) else stillFrame(animation, time, item)
                    drawFrame(breed.sheet, frame, x - art.anchorX, standing(y, breed.sheet, animation), flip = walk?.facingLeft == true)
                }
            }
            ItemKind.Huevo, ItemKind.Pescado -> {
                val animation = if (item.kind == ItemKind.Huevo) art.huevo else art.pescado
                val leap = flight as? Leap
                val leaping = art.leap
                if (leap != null && leaping != null) {
                    // Out of the water only (GameMaster): its pose by how fast it goes up or down, turned for leaps to the left.
                    val climb = leap.climb(reach)
                    val third = leap.rise / LEAP_POSES
                    val pose = when {
                        climb > third -> LEAP_UP
                        climb >= 0.0 -> LEAP_TOP
                        climb >= -third -> LEAP_TURN
                        else -> LEAP_DOWN
                    }
                    clipRect(bottom = waterRow.toFloat()) {
                        drawFrame(art.sheet, leaping.frame(pose), x - art.anchorX, y - art.anchorY, flip = leap.towardsLeft(reach))
                    }
                    continue
                }
                if (animation == null) {
                    paintGreyItem(item, x, y)
                } else {
                    drawFrame(art.sheet, animation.frameAt(time, loops = true), x - art.anchorX, y - art.anchorY)
                }
                // The smell over what takes points away, a little above its centre (Fangusk, through GameMaster).
                val smell = art.smell
                if (smell != null) {
                    val frame = smell.frameAt(time, loops = true)
                    drawFrame(art.smellSheet, frame, x - art.smellAnchorX, y - SMELL_ABOVE - art.smellAnchorY)
                }
            }
            ItemKind.Pajaro -> {
                val bird = art.birds[run.scenario.ordinal]
                if (bird == null) {
                    paintGreyItem(item, x, y)
                } else {
                    // Those that fly at her beat their wings; those that stroll glide, and beat them to turn (GameMaster).
                    val stroll = flight as? Stroll
                    val animation = if (stroll == null || stroll.turning(reach)) bird.flap else bird.glide
                    val left = flight?.towardsLeft(reach) != false
                    drawFrame(art.sheet, animation.frameAt(time, loops = true), x - art.anchorX, y - art.anchorY, flip = !left)
                }
            }
        }
    }
}

/**
 * The sky and the two layers of the background (TEC-04, TEC-18), straight on the screen behind the world like the grey
 * background: the sky still, the far layer at 30 % of the camera and the middle one at 60 % ([LAYER_SPEEDS]), each with its
 * own offset rounded to a pixel of the phone, so they glide instead of stepping. At the start of a run, with the camera at
 * [restCamera], they sit on the game's pixel grid. Their bottoms rest on the line of the lowest ground, less the rows
 * that go under it, which show inside the gaps.
 */
internal fun DrawScope.paintScenery(
    scenery: ScenarioArt,
    camera: Double,
    restCamera: Double,
    cameraBottom: Int,
    viewport: PixelViewport,
    groundLine: Int,
    millis: Long,
    plan: BackgroundPlan?,
) {
    val scale = viewport.scale
    val sky = scenery.sky
    if (sky != null) {
        drawImage(
            sky,
            IntOffset.Zero,
            IntSize(sky.width, sky.height),
            IntOffset(viewport.offsetX, viewport.offsetY + (viewport.height - sky.height) * scale),
            IntSize(viewport.width * scale, sky.height * scale),
            filterQuality = FilterQuality.None,
        )
    }
    scenery.far?.let { paintLayer(it, FAR, camera, restCamera, cameraBottom, viewport, groundLine, plan) }
    scenery.mid?.let { paintLayer(it, MID, camera, restCamera, cameraBottom, viewport, groundLine, plan) }
    scenery.weather?.let { paintWeather(it.at(millis), camera, restCamera, cameraBottom, viewport) }
}

/** The mosaic of a decoration in the air over the whole screen, at [WEATHER_SPEED] of the camera. */
private fun DrawScope.paintWeather(
    image: ImageBitmap,
    camera: Double,
    restCamera: Double,
    cameraBottom: Int,
    viewport: PixelViewport,
) {
    val scale = viewport.scale
    val shift = (camera - restCamera) * WEATHER_SPEED + floor(restCamera * WEATHER_SPEED)
    val period = image.width.toLong() * scale
    val first = floor(cameraBottom * WEATHER_SPEED).toInt().mod(image.height) - image.height
    var x = -(shift * scale).roundToLong().mod(period)
    while (x < viewport.width.toLong() * scale) {
        var y = first
        while (y < viewport.height) {
            drawImage(
                image,
                IntOffset.Zero,
                IntSize(image.width, image.height),
                IntOffset(viewport.offsetX + x.toInt(), viewport.offsetY + y * scale),
                IntSize(image.width * scale, image.height * scale),
                filterQuality = FilterQuality.None,
            )
            y += image.height
        }
        x += period
    }
}

/**
 * A layer of the background, round after round of its strip at its own speed; each round, its base or the variant that
 * [plan] gives it ([BackgroundPlan]). [index] is which layer it is: [FAR] or [MID].
 */
private fun DrawScope.paintLayer(
    layer: Layer,
    index: Int,
    camera: Double,
    restCamera: Double,
    cameraBottom: Int,
    viewport: PixelViewport,
    groundLine: Int,
    plan: BackgroundPlan?,
) {
    val scale = viewport.scale
    val speed = LAYER_SPEEDS[index]
    val base = layer.image
    val shift = (camera - restCamera) * speed + floor(restCamera * speed)
    val period = base.width.toLong() * scale
    val top = viewport.height + floor(cameraBottom * speed).toInt() - (groundLine - layer.belowGround) - base.height
    val shiftPixels = (shift * scale).roundToLong()
    var lap = shiftPixels.floorDiv(period)
    var x = -shiftPixels.mod(period)
    while (x < viewport.width.toLong() * scale) {
        val variant = plan?.variant(index, lap) ?: -1
        val image = if (variant >= 0) layer.variants[variant].image else base
        drawImage(
            image,
            IntOffset.Zero,
            IntSize(image.width, image.height),
            IntOffset(viewport.offsetX + x.toInt(), viewport.offsetY + top * scale),
            IntSize(image.width * scale, image.height * scale),
            filterQuality = FilterQuality.None,
        )
        x += period
        lap++
    }
}

/**
 * The plan of when the far and the middle layer of [scenery] show their variants, for a run of [seed], or null if they
 * have none. [restCamera] and [speedAt] as BackgroundPlan takes them.
 */
internal fun backgroundPlan(
    scenery: ScenarioArt,
    seed: Long,
    restCamera: Double,
    speedAt: (Double) -> Double,
): BackgroundPlan? {
    val layers = listOf(scenery.far, scenery.mid).mapIndexed { index, layer ->
        PlanLayer(LAYER_SPEEDS[index], layer?.image?.width ?: 1, layer?.variants?.map { it.from until it.to }.orEmpty())
    }
    if (layers.all { it.variants.isEmpty() }) return null
    return BackgroundPlan(seed, layers, restCamera, speedAt)
}

/**
 * Caro with her art (CHR-01 to CHR-07), her feet's centre at ([x], [feet]) in game pixels from the top left of the screen:
 * the pose of [look], with the face of [reaction] and the clothes of [scenario].
 */
internal fun DrawScope.paintCaroSprite(
    art: CaroArt,
    scenario: Scenario,
    look: CaroLook,
    reaction: Reaction,
    x: Int,
    feet: Int,
) {
    val animation = art.animation(look.pose, reaction)
    drawFrame(art.sheet(scenario), look.frame(animation), x - art.anchorX, feet - art.anchorY)
}

/**
 * The top row for an animal whose centre is at the buffer row [y]: the last row of its drawing on what it stands on,
 * whatever the size of its frame.
 */
private fun standing(y: Int, sheet: Sheet, animation: Animation): Int = y + Item.ABOVE_GROUND - (sheet.height - animation.emptyBelow)

/**
 * The frame of a perrito standing still: its first frame lasts 1 to 3 s longer each round, a different time for each
 * perrito, so they do not pant and blink all the time nor all at once (GameMaster's advice with the art).
 */
private fun stillFrame(animation: Animation, time: Long, item: Item): Int {
    val rest = REST_MILLIS + 2 * outOfStep(item)
    val round = (time - outOfStep(item)).mod(animation.duration + rest)
    return if (round < rest) animation.frame(0) else animation.frameAt(round - rest, loops = false)
}

/**
 * Caro celebrating a new record at both sides of the end [panel], facing us (the record animation GameMaster drew for
 * that panel), her feet on its bottom edge; where the panel goes from side to side of the screen [width] wide, over it.
 */
internal fun DrawScope.paintRecordCelebration(
    art: CaroArt,
    scenario: Scenario,
    millis: Long,
    panel: Box,
    width: Int,
) {
    val animation = art.animation(CaroPose.Record, Reaction.None)
    val frame = animation.frameAt(millis, loops = true)
    val sheet = art.sheet(scenario)
    val left = if (panel.left >= CELEBRATION_ROOM) panel.left - CELEBRATION_BESIDE else CELEBRATION_X
    val right = if (width - panel.right >= CELEBRATION_ROOM) panel.right + CELEBRATION_BESIDE else width - CELEBRATION_X
    drawFrame(sheet, frame, left - art.anchorX, panel.bottom - art.anchorY)
    drawFrame(sheet, frame, right - art.anchorX, panel.bottom - art.anchorY, flip = true)
}

/** A fixed shift of each collectible's animations, so a row of them does not move all at once. */
private fun outOfStep(item: Item): Long = ((item.x * 2_654_435_761L + item.y * 40_503L) ushr 8) % OUT_OF_STEP_MILLIS

/** Which of two tiles goes at a column and a row: fixed, so the ground looks the same every time it comes round. */
private fun variation(column: Int, row: Int): Int {
    var h = column * 0x27D4EB2D xor row * 0x165667B1
    h = h xor (h ushr 15)
    h *= 0x2C1B3C6D
    return (h xor (h ushr 12)) and 0xFFFF
}

/** How far from each side of the screen Caro celebrates a record, in game pixels. */
private const val CELEBRATION_X = 34

/** Beside a panel: from its edge to her middle, and the room she needs there. */
private const val CELEBRATION_BESIDE = 20
private const val CELEBRATION_ROOM = 36

/** The poses of the fish that leaps, by their frames in its animation (GameMaster): up, arched at the top, down and turning. */
private const val LEAP_UP = 0
private const val LEAP_TOP = 1
private const val LEAP_DOWN = 2
private const val LEAP_TURN = 3

/** Its pose changes at a third of how fast it leaves the water, going up and coming down. */
private const val LEAP_POSES = 3

/** How far above its centre the smell sits over the egg and the fish, in game pixels. */
private const val SMELL_ABOVE = 6
private const val OUT_OF_STEP_MILLIS = 1000L
private const val REST_MILLIS = 1000L

/** The far and the middle layer, by their place in [LAYER_SPEEDS] and in the [BackgroundPlan]. */
private const val FAR = 0
private const val MID = 1

/** How fast each layer moves with the camera: the far one at 30 %, the middle one at 60 %. */
internal val LAYER_SPEEDS = doubleArrayOf(0.3, 0.6)

/** The decoration in the air moves with the ground, as GameMaster made it. */
private const val WEATHER_SPEED = 1.0

/** One surface tile in this many, more or less, has the scenery's plant on it. */
private const val PLANT_EVERY = 6
private const val PLANT_ROW = 7
