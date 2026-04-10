package com.gymbro.core.voice

import com.gymbro.core.preferences.UserPreferences

data class ParsedVoiceInput(
    val weight: Double,
    val reps: Int,
    val unit: UserPreferences.WeightUnit,
    val rpe: Double? = null,
)

class VoiceInputParser {
    
    private val numberWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
        "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19, "twenty" to 20,
        "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
        "eighty" to 80, "ninety" to 90, "hundred" to 100,
        // Spanish numbers
        "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "veinte" to 20, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
        "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90,
    )

    fun parse(transcript: String, defaultUnit: UserPreferences.WeightUnit = UserPreferences.WeightUnit.KG): ParsedVoiceInput? {
        val normalized = transcript.lowercase().trim()
        
        // Extract RPE before removing it from the string for weight/reps parsing
        val rpe = extractRpe(normalized)
        val withoutRpe = removeRpeSegment(normalized)

        // Extract unit from keywords
        val unit = when {
            withoutRpe.contains("kilo") || withoutRpe.contains("kg") -> UserPreferences.WeightUnit.KG
            withoutRpe.contains("pound") || withoutRpe.contains("lbs") || withoutRpe.contains("lb") -> UserPreferences.WeightUnit.LBS
            else -> defaultUnit
        }

        // Extract numbers
        val numbers = extractNumbers(withoutRpe)
        if (numbers.isEmpty()) return null

        // Pattern matching for different formats
        // "100 kilos 5 reps", "225 pounds 3 reps", "5 reps at 100", "100x5", "100 for 5"
        
        var weight: Double? = null
        var reps: Int? = null

        // Check for "x" format: "100x5" or "100 x 5"
        val xPattern = Regex("""(\d+\.?\d*)\s*[x×]\s*(\d+)""")
        val xMatch = xPattern.find(withoutRpe)
        if (xMatch != null) {
            weight = xMatch.groupValues[1].toDoubleOrNull()
            reps = xMatch.groupValues[2].toIntOrNull()
        }

        // Check for "for" format: "100 for 5"
        if (weight == null || reps == null) {
            val forPattern = Regex("""(\d+\.?\d*)\s+for\s+(\d+)""")
            val forMatch = forPattern.find(withoutRpe)
            if (forMatch != null) {
                weight = forMatch.groupValues[1].toDoubleOrNull()
                reps = forMatch.groupValues[2].toIntOrNull()
            }
        }

        // Check for "at" format: "5 reps at 100"
        if (weight == null || reps == null) {
            val atPattern = Regex("""(\d+)\s+(?:reps?)?\s*at\s+(\d+\.?\d*)""")
            val atMatch = atPattern.find(withoutRpe)
            if (atMatch != null) {
                reps = atMatch.groupValues[1].toIntOrNull()
                weight = atMatch.groupValues[2].toDoubleOrNull()
            }
        }

        // Default: first larger number is weight, second smaller number is reps
        if (weight == null || reps == null) {
            if (numbers.size >= 2) {
                // Heuristic: weight is usually larger than reps
                if (numbers[0] > numbers[1]) {
                    weight = numbers[0]
                    reps = numbers[1].toInt()
                } else {
                    weight = numbers[1]
                    reps = numbers[0].toInt()
                }
            } else if (numbers.size == 1) {
                // Only one number - check context
                if (withoutRpe.contains("rep")) {
                    reps = numbers[0].toInt()
                } else {
                    weight = numbers[0]
                }
            }
        }

        // Validate and return
        if (weight != null && weight > 0 && reps != null && reps > 0) {
            return ParsedVoiceInput(weight, reps, unit, rpe)
        }

        return null
    }

    private fun extractNumbers(text: String): List<Double> {
        val numbers = mutableListOf<Double>()
        
        // Extract digit-based numbers
        val digitPattern = Regex("""(\d+\.?\d*)""")
        digitPattern.findAll(text).forEach { match ->
            match.value.toDoubleOrNull()?.let { numbers.add(it) }
        }

        // Extract word-based numbers
        val words = text.split(Regex("""\s+"""))
        for (word in words) {
            numberWords[word]?.let { num ->
                if (numbers.isEmpty() || num > 20) {
                    numbers.add(num.toDouble())
                }
            }
        }

        return numbers.distinct()
    }

    fun formatConfirmation(input: ParsedVoiceInput): String {
        val unitSymbol = when (input.unit) {
            UserPreferences.WeightUnit.KG -> "kg"
            UserPreferences.WeightUnit.LBS -> "lbs"
        }
        val base = "${input.weight.toInt()}$unitSymbol × ${input.reps}"
        return if (input.rpe != null) {
            "$base @ RPE ${formatRpe(input.rpe)}"
        } else {
            base
        }
    }

    private fun formatRpe(rpe: Double): String {
        return if (rpe == rpe.toLong().toDouble()) rpe.toLong().toString() else rpe.toString()
    }

    /**
     * Extracts RPE value from phrases like "RPE 8", "RPE ocho", "rpe 9.5".
     * Supports English and Spanish number words.
     */
    private fun extractRpe(text: String): Double? {
        // Match "rpe" followed by a number (digit or word)
        val digitPattern = Regex("""rpe\s+(\d+\.?\d*)""")
        val digitMatch = digitPattern.find(text)
        if (digitMatch != null) {
            val value = digitMatch.groupValues[1].toDoubleOrNull()
            if (value != null && value in 1.0..10.0) return value
        }

        // Match "rpe" followed by a number word
        val wordPattern = Regex("""rpe\s+(\w+)""")
        val wordMatch = wordPattern.find(text)
        if (wordMatch != null) {
            val word = wordMatch.groupValues[1]
            val value = numberWords[word]
            if (value != null && value in 1..10) return value.toDouble()
        }

        return null
    }

    /**
     * Removes the RPE segment from input so RPE numbers don't interfere
     * with weight/reps extraction.
     */
    private fun removeRpeSegment(text: String): String {
        // Remove "rpe <number>" or "rpe <word>"
        return text
            .replace(Regex("""rpe\s+\d+\.?\d*"""), "")
            .replace(Regex("""rpe\s+\w+"""), "")
            .trim()
    }
}
