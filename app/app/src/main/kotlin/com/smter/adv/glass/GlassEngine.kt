package com.smter.adv.glass

/**
 * Which rendering tier the glass pane is allowed to use.
 *
 * The platform decides by default — a runtime shader needs API 33, render effects need API 31 —
 * and the decision lives here, inside the glass module, so call sites never ask the platform what
 * it can do. A debug launch extra may force a lower tier on a capable device so the fallbacks can
 * be captured in screenshots.
 */
enum class GlassEngine {
    /** API 33+: the library's runtime-shader lens refracts the backdrop. */
    FULL,

    /** API 31–32: no runtime shader; blur, tone and the rim stand in for refraction. */
    BLUR,

    /** API 26–30: no render effects either; an opaque tinted pane with its own rim. */
    OPAQUE,
    ;

    companion object {
        /** The strongest tier the given platform can run. */
        fun forSdk(sdkInt: Int): GlassEngine = when {
            sdkInt >= 33 -> FULL
            sdkInt >= 31 -> BLUR
            else -> OPAQUE
        }

        /** A debug override wins; otherwise the platform decides. */
        fun resolve(sdkInt: Int, forced: GlassEngine?): GlassEngine = forced ?: forSdk(sdkInt)

        /**
         * Parses the `engine` launcher extra. `fallback` is kept as an alias for [BLUR] so the
         * documented screenshot recipe keeps working; anything else means "let the platform decide".
         */
        fun parse(raw: String?): GlassEngine? = when (raw?.trim()?.lowercase()) {
            "full" -> FULL
            "blur", "fallback" -> BLUR
            "opaque" -> OPAQUE
            else -> null
        }
    }
}
