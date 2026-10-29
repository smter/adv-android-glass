package com.smter.adv.glass

/**
 * AGSL programs (API 33+) that implement the upstream liquid glass in one pass.
 *
 * The upstream page built a 300x200 displacement bitmap in JavaScript and fed it to
 * feDisplacementMap. AGSL can evaluate the very same signed-distance recipe per pixel on the GPU,
 * which also keeps the field correct while the panel is animating its height.
 */
object LiquidGlassShaders {

    /** SDF + double smooth-step, kept identical to [LensGeometry]. */
    private const val SDF = """
        float len2(float x, float y) { return sqrt(x * x + y * y); }
        float smoothStep(float a, float b, float t) {
            float x = clamp((t - a) / (b - a), 0.0, 1.0);
            return x * x * (3.0 - 2.0 * x);
        }
        float scaleAt(float2 uv) {
            float2 p = uv - 0.5;
            float qx = abs(p.x) - uHalfWidth + uCornerRadius;
            float qy = abs(p.y) - uHalfHeight + uCornerRadius;
            float d = min(max(qx, qy), 0.0) + len2(max(qx, 0.0), max(qy, 0.0)) - uCornerRadius;
            return smoothStep(0.0, 1.0, smoothStep(uRamp, 0.0, d - uFeather));
        }
    """

    /**
     * Lens variant. Samples the wallpaper that sits behind the pill. The input shader is handed in by
     * colorFilter, so the whole effect runs as one color call on the panel's own draw pass.
     */
    val LENS = """
        uniform shader content;
        uniform float2 uPanelSize;
        uniform float2 uLayerOrigin;   // top-left of the sample layer, in panel pixels
        uniform float2 uLayerScale;    // sample-layer px per panel px
        uniform float uDisplacement;   // px of pull at the strongest point of the rim
        uniform float2 uCornerPx;
        uniform float uHalfWidth;
        uniform float uHalfHeight;
        uniform float uCornerRadius;
        uniform float uFeather;
        uniform float uRamp;

        $SDF

        half4 main(float2 p) {
            float2 uv = p / uPanelSize;
            float s = scaleAt(uv);
            float2 centre = uPanelSize * 0.5;
            float2 sample = centre + (p - centre) * s;
            sample = clamp(sample, uCornerPx, uPanelSize - uCornerPx - float2(1.0));
            float2 mapped = uLayerOrigin + sample * uLayerScale;
            return content.eval(mapped);
        }
    """

    /**
     * Diamond variant. A cheap analytic stand-in for liquid-diamond.js: a faceted prism whose faces
     * each carry their own refraction vector, plus a bright rim and cyan bloom. Matches the poster
     * composition of the upstream demo (red prism, cyan flare around it) rather than its ray tracer,
     * so the panel stays at frame rate while it is being resized.
     */
    val DIAMOND = """
        uniform shader content;
        uniform float2 uPanelSize;
        uniform float2 uLayerOrigin;
        uniform float2 uLayerScale;
        uniform float uDisplacement;
        uniform float2 uCornerPx;
        uniform float2 uApex;      // apex of the prism, in panel px
        uniform float uPrismH;     // prism height in panel px
        uniform float uPrismW;     // prism half width
        uniform float uTime;
        uniform float uHalfWidth;
        uniform float uHalfHeight;
        uniform float uCornerRadius;
        uniform float uFeather;
        uniform float uRamp;

        float len2(float x, float y) { return sqrt(x * x + y * y); }

        float distanceToSegment(float2 p, float2 a, float2 b) {
            float2 ab = b - a;
            float t = clamp(dot(p - a, ab) / max(dot(ab, ab), 0.0001), 0.0, 1.0);
            return len2(p.x - (a.x + ab.x * t), p.y - (a.y + ab.y * t));
        }

        // Facet weight: distance to the nearest prism edge, normalised by the prism size.
        float facet(float2 p) {
            float2 a = uApex;
            float2 b = a + float2(-uPrismW, uPrismH);
            float2 c = a + float2(uPrismW, uPrismH);
            float dAB = distanceToSegment(p, a, b);
            float dAC = distanceToSegment(p, a, c);
            float dBC = distanceToSegment(p, b, c);
            return min(min(dAB, dAC), dBC) / uPrismW;
        }

        half4 main(float2 p) {
            float2 uv = p / uPanelSize;
            // Rounded-rect rim shared with the lens program: keeps the band edge behaving like glass.
            float2 q = uv - 0.5;
            float qx = abs(q.x) - uHalfWidth + uCornerRadius;
            float qy = abs(q.y) - uHalfHeight + uCornerRadius;
            float sd = min(max(qx, qy), 0.0) + len2(max(qx, 0.0), max(qy, 0.0)) - uCornerRadius;
            float lensRim = smoothstep(0.0, 1.0, smoothstep(uRamp, 0.0, sd - uFeather));
            float f = facet(p);
            float rim = max(smoothstep(0.16, 0.0, f), 1.0 - lensRim);
            float2 centre = uPanelSize * 0.5;
            float2 dir = p - centre;
            // Each facet bends along a slightly different direction, which reads as a cut crystal.
            float2 axis = normalize(float2(dir.x, dir.y * 1.4) + 0.0001);
            float bend = rim * uDisplacement * 2.2;
            float2 base = p + axis * bend;
            float2 mapped = uLayerOrigin + clamp(base, uCornerPx, uPanelSize - uCornerPx) * uLayerScale;
            half4 col = content.eval(mapped);

            // Facet brightening + specular streaks like the poster illustration.
            float streak = pow(clamp(1.0 - abs((uv.x - uv.y) * 2.0 - 0.2), 0.0, 1.0), 8.0);
            float3 tint = float3(0.62, 0.86, 1.0);
            col.rgb = mix(col.rgb, tint, 0.10 * rim + 0.22 * streak);
            col.rgb += tint * 0.16 * rim;
            return col;
        }
    """
}
