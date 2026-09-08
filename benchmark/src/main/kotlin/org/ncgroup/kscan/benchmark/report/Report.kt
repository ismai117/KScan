package org.ncgroup.kscan.benchmark.report

import org.ncgroup.kscan.benchmark.corpus.Sample

/**
 * What a decoder made of one frame.
 *
 * [MISREAD] is kept apart from [MISS] because a scanner that returns the wrong
 * payload is worse than one that returns nothing.
 */
enum class Outcome {
    HIT,
    MISREAD,
    MISS,
}

data class Observation(
    val engine: String,
    val format: String,
    val condition: String,
    val group: String,
    val pixelsPerModule: Int,
    val outcome: Outcome,
    val decodedFormat: String?,
    val decodedText: String?,
    val elapsedMicros: Long,
)

class Tally(
    val total: Int,
    val hits: Int,
    val misreads: Int,
) {
    val misses: Int get() = total - hits - misreads
    val hitRate: Double get() = if (total == 0) 0.0 else hits * 100.0 / total
    val misreadRate: Double get() = if (total == 0) 0.0 else misreads * 100.0 / total

    companion object {
        fun of(observations: Collection<Observation>): Tally = Tally(
            total = observations.size,
            hits = observations.count { it.outcome == Outcome.HIT },
            misreads = observations.count { it.outcome == Outcome.MISREAD },
        )
    }
}

/** Renders the observations as the Markdown table that goes in the pull request. */
object Report {
    private const val REGRESSION_THRESHOLD = 5.0

    fun render(
        title: String,
        environment: Map<String, String>,
        engines: List<String>,
        observations: List<Observation>,
    ): String {
        val baseline = engines.first()
        val builder = StringBuilder()

        builder.appendLine("# $title").appendLine()
        environment.forEach { (key, value) -> builder.appendLine("- **$key**: $value") }
        builder.appendLine()

        builder.appendLine("## Overall").appendLine()
        builder.appendLine("| Engine | Frames | Read correctly | Misread | Missed | Median ms | p90 ms |")
        builder.appendLine("|---|---:|---:|---:|---:|---:|---:|")
        engines.forEach { engine ->
            val forEngine = observations.filter { it.engine == engine }
            val tally = Tally.of(forEngine)
            val times = forEngine.map { it.elapsedMicros }.sorted()
            builder.appendLine(
                "| $engine | ${tally.total} | ${percent(tally.hitRate)} (${tally.hits}) | " +
                    "${percent(tally.misreadRate)} (${tally.misreads}) | ${tally.misses} | " +
                    "${millis(percentile(times, 50))} | ${millis(percentile(times, 90))} |",
            )
        }
        builder.appendLine()

        builder.append(breakdown("Read correctly, by format", engines, observations, baseline) { it.format })
        builder.append(breakdown("Read correctly, by condition group", engines, observations, baseline) { it.group })
        builder.append(breakdown("Read correctly, by condition", engines, observations, baseline) { it.condition })

        engines.drop(1).forEach { engine ->
            builder.append(regressions(baseline, engine, observations))
        }

        builder.append(misreadDetail(observations))

        return builder.toString()
    }

    fun csv(observations: List<Observation>): String {
        val builder = StringBuilder()
        builder.appendLine("engine,format,condition,group,pixels_per_module,outcome,decoded_format,decoded_text,micros")
        observations.forEach {
            builder.appendLine(
                listOf(
                    it.engine,
                    it.format,
                    it.condition,
                    it.group,
                    it.pixelsPerModule.toString(),
                    it.outcome.name,
                    it.decodedFormat.orEmpty(),
                    it.decodedText.orEmpty(),
                    it.elapsedMicros.toString(),
                ).joinToString(",") { field -> quote(field) },
            )
        }
        return builder.toString()
    }

    private fun breakdown(
        heading: String,
        engines: List<String>,
        observations: List<Observation>,
        baseline: String,
        key: (Observation) -> String,
    ): String {
        val builder = StringBuilder()
        val keys = observations.map(key).distinct()
        val comparisons = engines.drop(1)

        builder.appendLine("## $heading").appendLine()
        builder.append("| | ")
        builder.append(engines.joinToString(" | "))
        comparisons.forEach { builder.append(" | $it delta") }
        builder.appendLine(" |")
        builder.appendLine("|---|" + "---:|".repeat(engines.size + comparisons.size))

        keys.forEach { value ->
            val forKey = observations.filter { key(it) == value }
            val rates = engines.associateWith { engine -> Tally.of(forKey.filter { it.engine == engine }).hitRate }

            builder.append("| $value | ")
            builder.append(engines.joinToString(" | ") { percent(rates.getValue(it)) })
            comparisons.forEach { engine ->
                builder.append(" | ${delta(rates.getValue(engine) - rates.getValue(baseline))}")
            }
            builder.appendLine(" |")
        }

        return builder.appendLine().toString()
    }

    private fun regressions(
        baseline: String,
        engine: String,
        observations: List<Observation>,
    ): String {
        val builder = StringBuilder()
        val rows =
            observations
                .groupBy { it.format to it.condition }
                .mapNotNull { (key, group) ->
                    val baselineRate = Tally.of(group.filter { it.engine == baseline }).hitRate
                    val engineRate = Tally.of(group.filter { it.engine == engine }).hitRate
                    val difference = engineRate - baselineRate

                    if (difference <= -REGRESSION_THRESHOLD) Triple(key, baselineRate, engineRate) else null
                }.sortedBy { it.third - it.second }

        builder.appendLine("## Where $engine does not read a frame $baseline reads").appendLine()

        if (rows.isEmpty()) {
            builder.appendLine("None.").appendLine()
            return builder.toString()
        }

        builder.appendLine("| Format | Condition | $baseline | $engine |")
        builder.appendLine("|---|---|---:|---:|")
        rows.forEach { (key, baselineRate, engineRate) ->
            builder.appendLine("| ${key.first} | ${key.second} | ${percent(baselineRate)} | ${percent(engineRate)} |")
        }

        return builder.appendLine().toString()
    }

    private fun misreadDetail(observations: List<Observation>): String {
        val misreads = observations.filter { it.outcome == Outcome.MISREAD }
        val builder = StringBuilder()

        builder.appendLine("## Misreads").appendLine()

        if (misreads.isEmpty()) {
            builder.appendLine("None.").appendLine()
            return builder.toString()
        }

        builder.appendLine("| Engine | Format | Condition | Reported as | Reported text |")
        builder.appendLine("|---|---|---|---|---|")
        misreads.forEach {
            builder.appendLine(
                "| ${it.engine} | ${it.format} | ${it.condition} | ${it.decodedFormat} | `${it.decodedText}` |",
            )
        }

        return builder.appendLine().toString()
    }

    private fun percentile(
        sorted: List<Long>,
        percentile: Int,
    ): Long {
        if (sorted.isEmpty()) return 0
        val index = ((sorted.size - 1) * percentile / 100.0).toInt()
        return sorted[index]
    }

    private fun percent(value: Double): String = String.format("%.1f%%", value)

    private fun millis(micros: Long): String = String.format("%.1f", micros / 1000.0)

    private fun delta(value: Double): String = String.format("%+.1f", value)

    private fun quote(field: String): String = "\"" + field.replace("\"", "\"\"").replace("\n", "\\n") + "\""
}

/**
 * A decoder that returns nothing is counted apart from one that returns the wrong
 * payload, so a symbology whose text convention simply differs cannot be mistaken
 * for a decoder that reads a frame incorrectly.
 */
fun outcomeOf(
    sample: Sample,
    decodedText: String?,
): Outcome = when {
    decodedText == null -> Outcome.MISS
    sample.matches(decodedText) -> Outcome.HIT
    else -> Outcome.MISREAD
}
