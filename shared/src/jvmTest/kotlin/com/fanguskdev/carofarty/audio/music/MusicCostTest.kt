package com.fanguskdev.carofarty.audio.music

import com.fanguskdev.carofarty.game.Scenario
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the music costs the audio thread (AUD-25): time, and no garbage at all. */
class MusicCostTest {
    private val rate = 48_000

    @Test
    fun aMinuteOfMusicRendersInWellUnderASecond() {
        for (scenario in Scenario.entries) {
            val stream = Runs.stream(scenario, seed = 11, rate = rate)
            stream.setLevel(4)
            val buffer = ShortArray(960)
            repeat(250) { stream.render(buffer, 0, buffer.size) } // 5 s to warm up the JIT
            val frames = 60 * rate
            val start = System.nanoTime()
            var done = 0
            while (done < frames) {
                stream.render(buffer, 0, buffer.size)
                done += buffer.size
            }
            val millis = (System.nanoTime() - start) / 1e6
            println("${scenario.name}: 60 s of music at $rate Hz in %.0f ms".format(millis))
            assertTrue(millis < 1000, "${scenario.name}: a minute took $millis ms")
        }
    }

    @Test
    fun theBankIsMadeQuickly() {
        for (scenario in Scenario.entries) {
            val start = System.nanoTime()
            MusicBank(scenario)
            val millis = (System.nanoTime() - start) / 1e6
            println("${scenario.name}: instruments made in %.0f ms".format(millis))
            assertTrue(millis < 2000, "${scenario.name}: the instruments took $millis ms")
        }
    }

    /**
     * A whole run allocates nothing, measured in a JVM of its own. The run is played twice and the second is measured:
     * the first time a path runs, the JVM itself allocates as it links the code, once per process. The JVM runs with
     * its first compiler only (C1): the optimizing one (C2) allocates a few bytes now and then when it deoptimizes a
     * method, whatever the code does, and that would hide what is being measured.
     */
    @Test
    fun renderingAllocatesNothing() {
        val java = ProcessHandle.current().info().command().orElse("java")
        val process = ProcessBuilder(
            java,
            "-XX:TieredStopAtLevel=1",
            "-Xmx512m",
            "-cp",
            System.getProperty("java.class.path"),
            AllocationCheck::class.java.name,
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        println(output.trim())
        assertEquals(0, exit, output)
    }
}

/** What [MusicCostTest.renderingAllocatesNothing] runs in its own JVM: prints the bytes of each scenario's second run. */
object AllocationCheck {
    @JvmStatic
    fun main(args: Array<String>) {
        val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val id = Thread.currentThread().threadId()
        val rate = 48_000
        val buffer = ShortArray(1024)
        var failed = false
        for (scenario in Scenario.entries) {
            val bank = MusicBank(scenario)
            var allocated = 0L
            repeat(2) {
                val stream = MusicStream(scenario, 3, rate, bank)
                val before = threads.getThreadAllocatedBytes(id)
                // Every path: the layers coming in and going out, new sections, the tense version and back, the top
                // level and its intensity cycle, a level down, the end and the silence after it.
                for (second in 0 until 200) {
                    when (second) {
                        20 -> stream.setLevel(2)
                        40 -> stream.setLevel(3)
                        60 -> stream.setLevel(4)
                        90 -> stream.setTense(true)
                        110 -> stream.setTense(false)
                        130 -> stream.setLevel(5)
                        170 -> stream.setLevel(1)
                        190 -> stream.end()
                    }
                    var done = 0
                    while (done < rate) {
                        stream.render(buffer, 0, buffer.size)
                        done += buffer.size
                    }
                }
                allocated = threads.getThreadAllocatedBytes(id) - before
            }
            println("${scenario.name}: the second run of 200 s allocated $allocated bytes")
            if (allocated != 0L) failed = true
        }
        System.exit(if (failed) 1 else 0)
    }
}
