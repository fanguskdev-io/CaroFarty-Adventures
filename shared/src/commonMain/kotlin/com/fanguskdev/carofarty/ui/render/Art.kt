package com.fanguskdev.carofarty.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import carofarty.shared.generated.resources.Res
import com.fanguskdev.carofarty.game.Reaction
import com.fanguskdev.carofarty.game.Scenario
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * The pixel art of the game (TEC-18), as tools/arte.py leaves it in `files/arte/` (TEC-11, docs/arte.md): Caro, the
 * collectibles and each scenario's tiles and backgrounds. Whatever is missing is drawn as the grey prototype, so the game
 * always starts, with or without art.
 */
internal class Art(
    val caro: CaroArt?,
    val items: ItemArt?,
    private val scenarios: Array<ScenarioArt?>,
    val ui: UiArt? = null,
) {
    fun scenario(scenario: Scenario): ScenarioArt? = scenarios[scenario.ordinal]

    companion object {
        private const val FOLDER = "files/arte"
        private val json = Json { ignoreUnknownKeys = true }

        /** The art in the resources, or null if there is none or it cannot be read. */
        suspend fun load(): Art? = runCatching { read() }.getOrNull()

        private suspend fun read(): Art {
            val index = json.decodeFromString(ArtIndex.serializer(), Res.readBytes("$FOLDER/indice.json").decodeToString())
            val images = HashMap<String, ImageBitmap>()

            suspend fun image(name: String): ImageBitmap = images.getOrPut(name) { Res.readBytes("$FOLDER/$name").decodeToImageBitmap() }

            suspend fun set(name: String): SpriteSet? {
                val entry = index.sets[name] ?: return null
                val (width, height) = entry.size
                val variants = entry.variants.map { Variant(it.family, it.name, Sheet(image(it.sheet), width, height, entry.columns)) }
                val animations = entry.animations
                    .filterValues { it.frames.isNotEmpty() && it.frames.size == it.millis.size }
                    .mapValues { (_, it) -> Animation(it.frames.toIntArray(), it.millis.toIntArray(), it.emptyBelow, it.cycleTiles) }
                val sheet = Sheet(image(entry.sheet), width, height, entry.columns)
                return SpriteSet(
                    sheet,
                    entry.anchor[0],
                    entry.anchor[1],
                    animations,
                    variants,
                    entry.breeds,
                    entry.characters,
                    entry.advance,
                )
            }

            val scenarios = arrayOfNulls<ScenarioArt>(Scenario.entries.size)
            for (scenario in Scenario.entries) {
                val entry = index.scenarios[scenario.key] ?: continue
                val tiles = set(entry.tiles) ?: continue
                val sky = entry.sky?.let { image(it.sheet) }
                suspend fun layer(entry: LayerEntry): Layer {
                    val base = image(entry.sheet)
                    return Layer(base, entry.belowGround, entry.variants.mapNotNull { (_, sheet) -> LayerVariant.of(base, image(sheet)) })
                }
                val far = entry.far?.let { layer(it) }
                val mid = entry.mid?.let { layer(it) }
                scenarios[scenario.ordinal] = ScenarioArt.of(tiles, sky, far, mid)
            }
            val caro = set("caro")?.let { CaroArt.of(it) }
            val items = set("recolectables")?.let { ItemArt(it, set("efectos")) }
            val ui = index.ui?.let { entry ->
                UiArt(
                    pieces = entry.pieces.mapValues { (_, it) -> UiPiece(image(it.sheet), it.slices, it.content, it.icon) },
                    colors = UiColors.of(entry.text),
                    icons = set("ui-iconos"),
                    digits = set("ui-cifras"),
                    medals = set("ui-medallas"),
                    dots = set("ui-puntos"),
                    meter = set("ui-medidor"),
                    title = entry.title?.let { image(it) },
                )
            }
            return Art(caro, items, scenarios, ui)
        }
    }
}

/** The name of a scenario in the art: its folder in arte/. */
internal val Scenario.key: String get() = name.lowercase()

/** A sheet of frames of [width] × [height] pixels, [columns] to a row, numbered from the top left. */
internal class Sheet(
    val image: ImageBitmap,
    val width: Int,
    val height: Int,
    val columns: Int,
)

/**
 * An animation: the frames of its sheet and how many milliseconds each lasts. [emptyBelow] is how many rows under the
 * drawing are transparent in every frame: what to lower it so it stands on the ground. [cycleTiles], for a run, how many
 * tiles a whole cycle of it covers, if the art says.
 */
internal class Animation(
    private val frames: IntArray,
    private val millis: IntArray,
    val emptyBelow: Int,
    val cycleTiles: Float? = null,
) {
    /** How long a round of it lasts, in milliseconds. */
    val duration = millis.sum()
    val size: Int get() = frames.size

    /** Its frame number [index], round and round. */
    fun frame(index: Int): Int = frames[index.mod(frames.size)]

    /** Its frame [time] milliseconds after it started: round and round if it [loops], or else it stays on the last one. */
    fun frameAt(time: Long, loops: Boolean): Int {
        if (duration <= 0) return frames[0]
        var left = if (loops) time.mod(duration.toLong()) else time.coerceIn(0L, duration - 1L)
        for (i in frames.indices) {
            left -= millis[i]
            if (left < 0) return frames[i]
        }
        return frames[frames.size - 1]
    }
}

/** A sheet with its animations, the point of a frame that goes where the thing is ([anchorX], [anchorY]) and its variants. */
internal class SpriteSet(
    val sheet: Sheet,
    val anchorX: Int,
    val anchorY: Int,
    private val animations: Map<String, Animation>,
    private val variants: List<Variant>,
    val breeds: List<String>,
    /** For a sheet of characters (the digits of the interface): which character each frame is, and how far apart. */
    val characters: String = "",
    val advance: Int = 0,
) {
    fun animation(name: String): Animation? = animations[name]

    /** The first animation whose name starts with [prefix], in the order of the art. */
    fun firstAnimation(prefix: String): Animation? = animations.entries.firstOrNull { it.key.startsWith(prefix) }?.value

    /** The same sheet in the colours of the variant [name] of [family]: "" for those without a family, like Caro's clothes. */
    fun variant(family: String, name: String): Sheet? = variants.firstOrNull { it.family == family && it.name == name }?.sheet

    /** The sheets of every variant of [family], in the order of the art. */
    fun family(family: String): List<Sheet> = variants.filter { it.family == family }.map { it.sheet }
}

internal class Variant(val family: String, val name: String, val sheet: Sheet)

/** Draws [frame] of [sheet] with its top left corner at ([left], [top]), turned to face the other way if [flip]. */
internal fun DrawScope.drawFrame(
    sheet: Sheet,
    frame: Int,
    left: Int,
    top: Int,
    flip: Boolean = false,
) {
    val source = IntOffset((frame % sheet.columns) * sheet.width, (frame / sheet.columns) * sheet.height)
    val size = IntSize(sheet.width, sheet.height)
    if (flip) {
        withTransform({ scale(-1f, 1f, Offset(left + sheet.width / 2f, top + sheet.height / 2f)) }) {
            drawImage(sheet.image, source, size, IntOffset(left, top), size, filterQuality = FilterQuality.None)
        }
    } else {
        drawImage(sheet.image, source, size, IntOffset(left, top), size, filterQuality = FilterQuality.None)
    }
}

/** Caro's poses (CHR-02 to CHR-06), by the names of docs/arte.md; [byDistance] for the runs, whose stride follows the ground. */
internal enum class CaroPose(val key: String, val loops: Boolean, val byDistance: Boolean = false) {
    Espera("espera", loops = true),
    Corre("corre", loops = true, byDistance = true),
    CorreRapido("corre-rapido", loops = true, byDistance = true),
    Salta("salta", loops = false),
    Apice("apice", loops = false),
    Cae("cae", loops = false),
    Aterriza("aterriza", loops = false),
    AlBarranco("al-barranco", loops = true),
    SinPuntos("sin-puntos", loops = false),
    Record("record", loops = true),

    /** The push back of an egg or a fish, with her face of disgust (Fangusk); without its art, the run with that face. */
    Retroceso("retroceso", loops = false),
}

/** Caro (CHR-01 to CHR-07): her poses with the faces she makes when she picks something up, dressed for each scenario. */
internal class CaroArt private constructor(set: SpriteSet, private val poses: Array<Array<Animation>>) {
    val anchorX = set.anchorX
    val anchorY = set.anchorY
    private val outfits = Array(Scenario.entries.size) { set.variant("", Scenario.entries[it].key) ?: set.sheet }

    fun sheet(scenario: Scenario): Sheet = outfits[scenario.ordinal]

    /** The animation of [pose] with the face of [reaction], or without it where the pose has none. */
    fun animation(pose: CaroPose, reaction: Reaction): Animation = poses[pose.ordinal][reaction.ordinal]

    companion object {
        /** Her art, or null without the run: a missing pose takes the one closest to it. */
        fun of(set: SpriteSet): CaroArt? {
            val run = set.animation(CaroPose.Corre.key) ?: return null
            val poses = Array(CaroPose.entries.size) { p ->
                val pose = CaroPose.entries[p]
                val plain = set.animation(pose.key) ?: closest(set, pose) ?: run
                Array(Reaction.entries.size) { r -> set.animation(pose.key + face(Reaction.entries[r])) ?: plain }
            }
            return CaroArt(set, poses)
        }

        private fun closest(set: SpriteSet, pose: CaroPose): Animation? = when (pose) {
            CaroPose.CorreRapido -> set.animation(CaroPose.Corre.key)
            CaroPose.Apice -> set.animation(CaroPose.Salta.key) ?: set.animation(CaroPose.Cae.key)
            CaroPose.Salta, CaroPose.AlBarranco -> set.animation(CaroPose.Cae.key)
            CaroPose.Cae -> set.animation(CaroPose.Salta.key)
            CaroPose.Record, CaroPose.SinPuntos -> set.animation(CaroPose.Espera.key)
            CaroPose.Retroceso -> set.animation(CaroPose.Corre.key + "-asco") ?: set.animation(CaroPose.Corre.key)
            else -> null
        }

        /** The suffix of the faces in the names of the animations (docs/arte.md). */
        private fun face(reaction: Reaction): String = when (reaction) {
            Reaction.None -> ""
            Reaction.Happy -> "-contenta"
            Reaction.Disgusted -> "-asco"
        }
    }
}

/**
 * The collectibles (GDD-10, ADR 0008): the gatito in its eight coats, the perrito in its nine breeds, and the egg and the
 * fish with the smell over them. An animation that is missing leaves that collectible in grey.
 */
internal class ItemArt(set: SpriteSet, effects: SpriteSet?) {
    val anchorX = set.anchorX
    val anchorY = set.anchorY
    val sheet = set.sheet
    val gatito = set.animation("gatito-quieto")
    val coats: Array<Sheet> = set.family("gatito").ifEmpty { listOf(set.sheet) }.toTypedArray()
    val huevo = set.animation("huevo")
    val pescado = set.animation("pescado")
    val breeds: Array<Breed> = set.breeds.mapNotNull { breed(set, it) }.toTypedArray()
    val smell = effects?.animation(SMELL) ?: set.animation(SMELL)
    val smellSheet = if (effects?.animation(SMELL) != null) effects.sheet else set.sheet
    val smellAnchorX = if (effects?.animation(SMELL) != null) effects.anchorX else set.anchorX
    val smellAnchorY = if (effects?.animation(SMELL) != null) effects.anchorY else set.anchorY

    private companion object {
        const val SMELL = "olor"

        fun breed(set: SpriteSet, name: String): Breed? {
            val walk = set.animation("perrito-$name-camina")
            val still = set.animation("perrito-$name-quieto")
            val sheet = set.variant("perrito", name) ?: set.sheet
            return when {
                walk != null && still != null -> Breed(walk, still, sheet)
                walk != null -> Breed(walk, walk, sheet)
                still != null -> Breed(still, still, sheet)
                else -> null
            }
        }
    }
}

/** A breed of perrito: walking and standing, drawn facing right, in its own colours. */
internal class Breed(val walk: Animation, val still: Animation, val sheet: Sheet)

/** A layer of the background that repeats sideways, with [belowGround] of its rows under the line of the ground. */
internal class Layer(val image: ImageBitmap, val belowGround: Int, val variants: List<LayerVariant> = emptyList())

/**
 * The same strip as a layer with something more, which the game puts now and then in place of a round of it (the cabin of
 * the snow): [image], and the columns from [from] to [to] where it differs from the base, where the something is.
 */
internal class LayerVariant private constructor(val image: ImageBitmap, val from: Int, val to: Int) {
    companion object {
        /** The variant [image] of [base], or null if it does not measure the same or does not differ from it. */
        fun of(base: ImageBitmap, image: ImageBitmap): LayerVariant? {
            if (image.width != base.width || image.height != base.height) return null
            val size = base.width * base.height
            val a = IntArray(size).also { base.readPixels(it) }
            val b = IntArray(size).also { image.readPixels(it) }
            var from = Int.MAX_VALUE
            var to = -1
            for (i in 0 until size) {
                if (a[i] == b[i]) continue
                val x = i % base.width
                if (x < from) from = x
                if (x > to) to = x
            }
            return if (to < 0) null else LayerVariant(image, from, to + 1)
        }
    }
}

/**
 * A scenario: the tiles of its ground (frames of [tiles]), its block in the air, its sky and its far and middle layers,
 * and its decoration if it has one: [plant], drawn on the ground here and there, or [weather], a mosaic of it in front of
 * the background and behind everything else, with each piece at another moment of the animation (GameMaster's copos).
 */
internal class ScenarioArt private constructor(
    val tiles: Sheet,
    val surface: IntArray,
    val fill: IntArray,
    val edgeLeft: Int,
    val edgeRight: Int,
    val fillLeft: Int,
    val fillRight: Int,
    val block: Int,
    val platform: PlatformTiles?,
    val mover: PlatformTiles?,
    val sky: ImageBitmap?,
    val far: Layer?,
    val mid: Layer?,
    val plant: Animation?,
    val weather: Weather?,
) {
    companion object {
        /** The scenario's art, or null without its surface and its fill. */
        fun of(
            set: SpriteSet,
            sky: ImageBitmap?,
            far: Layer?,
            mid: Layer?,
        ): ScenarioArt? {
            fun tile(name: String): Int? = set.animation(name)?.frame(0)
            val surface = listOfNotNull(tile("superficie-1"), tile("superficie-2"))
            val fill = listOfNotNull(tile("relleno-1"), tile("relleno-2"))
            if (surface.isEmpty() || fill.isEmpty()) return null
            // A decoration that reaches the bottom of its tile stands on the ground; one that does not floats in the air.
            val decoration = set.firstAnimation(DECORATION)
            val planted = decoration != null && decoration.emptyBelow == 0
            fun platform(name: String): PlatformTiles? {
                val middle = set.animation(name) ?: return null
                return PlatformTiles(set.animation("$name-izq") ?: middle, middle, set.animation("$name-der") ?: middle)
            }
            val platform = platform(PLATFORM)
            return ScenarioArt(
                tiles = set.sheet,
                surface = surface.toIntArray(),
                fill = fill.toIntArray(),
                edgeLeft = tile("borde-izq") ?: surface[0],
                edgeRight = tile("borde-der") ?: surface[0],
                fillLeft = tile("relleno-izq") ?: fill[0],
                fillRight = tile("relleno-der") ?: fill[0],
                block = tile("bloque") ?: fill[0],
                platform = platform,
                // Until the moving ones arrive, the fixed ones: they will still move.
                mover = platform(MOVER) ?: platform,
                sky = sky,
                far = far,
                mid = mid,
                plant = if (planted) decoration else null,
                weather = if (decoration != null && !planted) Weather.of(set.sheet, decoration) else null,
            )
        }

        private const val DECORATION = "decoracion-"

        /** The names of the platforms' tiles (ADR 0009), with -izq and -der for their ends. */
        private const val PLATFORM = "plataforma"
        private const val MOVER = "plataforma-movil"
    }
}

/**
 * The three tiles of a platform: its left end, its middle, which repeats, and its right end; each an animation, still for
 * the fixed ones and of two frames for the moving ones (GameMaster's leaf, sleigh and carpet).
 */
internal class PlatformTiles(val left: Animation, val middle: Animation, val right: Animation)

/**
 * A decoration in the air as a mosaic, as GameMaster draws the copos: staggered, every other tile in a row and the next
 * row shifted, each row nudged a few pixels and every tile at another phase so the grid does not show. One image of
 * [CELLS] × [CELLS] tiles for each moment of its animation, made once when the art loads, so drawing it takes a few images
 * a frame and not hundreds of tiles.
 */
internal class Weather private constructor(private val moments: Array<ImageBitmap>, private val frameMillis: Long) {
    val size: Int get() = moments[0].width

    /** The mosaic at [millis]. */
    fun at(millis: Long): ImageBitmap = moments[((millis / frameMillis) % moments.size).toInt()]

    companion object {
        private const val CELLS = 8

        fun of(sheet: Sheet, animation: Animation): Weather {
            val across = CELLS * sheet.width
            val tall = CELLS * sheet.height
            val moments = Array(animation.size) { moment ->
                val image = ImageBitmap(across, tall)
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(across.toFloat(), tall.toFloat())) {
                    for (row in 0 until CELLS) {
                        val nudge = (row * NUDGE_STEP) % sheet.width
                        for (column in row % 2 until CELLS step 2) {
                            val frame = animation.frame(moment + column + PHASE_ROW * row)
                            val x = column * sheet.width + nudge
                            drawFrame(sheet, frame, x, row * sheet.height)
                            // What goes out on the right comes in on the left, so the mosaic repeats without a seam.
                            if (x + sheet.width > across) drawFrame(sheet, frame, x - across, row * sheet.height)
                        }
                    }
                }
                image
            }
            return Weather(moments, (animation.duration / animation.size).toLong().coerceAtLeast(1L))
        }

        private const val PHASE_ROW = 3
        private const val NUDGE_STEP = 5
    }
}

/** indice.json, as tools/arte.py writes it. */
@Serializable
private class ArtIndex(
    @SerialName("conjuntos") val sets: Map<String, SetEntry> = emptyMap(),
    @SerialName("escenarios") val scenarios: Map<String, ScenarioEntry> = emptyMap(),
    val ui: UiEntry? = null,
)

@Serializable
private class UiEntry(
    @SerialName("piezas") val pieces: Map<String, PieceEntry> = emptyMap(),
    @SerialName("texto") val text: Map<String, String> = emptyMap(),
    @SerialName("titulo") val title: String? = null,
)

@Serializable
private class PieceEntry(
    @SerialName("hoja") val sheet: String,
    @SerialName("9-slice") val slices: List<Int>? = null,
    @SerialName("contenido") val content: List<Int>? = null,
    @SerialName("icono") val icon: List<Int>? = null,
)

@Serializable
private class SetEntry(
    @SerialName("hoja") val sheet: String,
    @SerialName("tamaño") val size: List<Int>,
    @SerialName("ancla") val anchor: List<Int>,
    @SerialName("columnas") val columns: Int,
    @SerialName("animaciones") val animations: Map<String, AnimationEntry> = emptyMap(),
    @SerialName("variantes") val variants: List<VariantEntry> = emptyList(),
    @SerialName("razas") val breeds: List<String> = emptyList(),
    @SerialName("caracteres") val characters: String = "",
    @SerialName("avance") val advance: Int = 0,
)

@Serializable
private class AnimationEntry(
    @SerialName("fotogramas") val frames: List<Int>,
    @SerialName("ms") val millis: List<Int>,
    @SerialName("vacias-abajo") val emptyBelow: Int = 0,
    @SerialName("ciclo-tiles") val cycleTiles: Float? = null,
)

@Serializable
private class VariantEntry(
    @SerialName("familia") val family: String = "",
    @SerialName("nombre") val name: String,
    @SerialName("hoja") val sheet: String,
)

@Serializable
private class ScenarioEntry(
    val tiles: String,
    @SerialName("cielo") val sky: LayerEntry? = null,
    @SerialName("lejano") val far: LayerEntry? = null,
    @SerialName("medio") val mid: LayerEntry? = null,
)

@Serializable
private class LayerEntry(
    @SerialName("hoja") val sheet: String,
    @SerialName("bajo-el-suelo") val belowGround: Int = 0,
    @SerialName("variantes") val variants: Map<String, String> = emptyMap(),
)
