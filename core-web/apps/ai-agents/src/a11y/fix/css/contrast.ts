/**
 * Deterministic WCAG contrast math — no LLM, no dependency.
 *
 * With the scanner returning axe's per-node `data` (fgColor, bgColor, the target
 * ratio), the contrast fix is pure math: nudge the failing color's lightness
 * until the pair clears the threshold, keeping the same hue. This removes the LLM
 * from the contrast path entirely (and the flakiness that came with it) and makes
 * every fix mathematically guaranteed to clear the violation.
 *
 * Refs: WCAG 2.1 relative luminance + contrast ratio.
 */

export interface Rgb {
    r: number; // 0..255
    g: number;
    b: number;
}

/** Parse #rgb / #rrggbb / rgb()/rgba() into Rgb, or null if unsupported. */
export function parseColor(input: string): Rgb | null {
    const s = input.trim().toLowerCase();
    const hex = s.match(/^#([0-9a-f]{3}|[0-9a-f]{6})$/);
    if (hex) {
        const h = hex[1];
        if (h.length === 3) {
            return {
                r: parseInt(h[0] + h[0], 16),
                g: parseInt(h[1] + h[1], 16),
                b: parseInt(h[2] + h[2], 16)
            };
        }
        return {
            r: parseInt(h.slice(0, 2), 16),
            g: parseInt(h.slice(2, 4), 16),
            b: parseInt(h.slice(4, 6), 16)
        };
    }
    const rgb = s.match(/^rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)/);
    if (rgb) {
        return { r: Number(rgb[1]), g: Number(rgb[2]), b: Number(rgb[3]) };
    }
    return null;
}

/** Format Rgb back to #rrggbb. */
export function toHex({ r, g, b }: Rgb): string {
    const h = (n: number) =>
        Math.max(0, Math.min(255, Math.round(n)))
            .toString(16)
            .padStart(2, '0');
    return `#${h(r)}${h(g)}${h(b)}`;
}

/** WCAG relative luminance (0..1). */
export function relativeLuminance({ r, g, b }: Rgb): number {
    const ch = (v: number) => {
        const c = v / 255;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    };
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b);
}

/** WCAG contrast ratio between two colors (1..21). */
export function contrastRatio(a: Rgb, b: Rgb): number {
    const la = relativeLuminance(a);
    const lb = relativeLuminance(b);
    const [hi, lo] = la >= lb ? [la, lb] : [lb, la];
    return (hi + 0.05) / (lo + 0.05);
}

/** Parse a target like "4.5:1" → 4.5 (default AA normal 4.5). */
export function parseTargetRatio(expected?: string): number {
    if (!expected) {
        return 4.5;
    }
    const m = expected.match(/([\d.]+)\s*:\s*1/);
    return m ? Number(m[1]) : 4.5;
}

/** Scale an Rgb's lightness toward black (factor<1) or white (factor>1, toward 255). */
function scaleToward(c: Rgb, target: 0 | 255, t: number): Rgb {
    const mix = (v: number) => v + (target - v) * t;
    return { r: mix(c.r), g: mix(c.g), b: mix(c.b) };
}

export interface ContrastFix {
    newColor: string; // the nudged color (#rrggbb)
    achievedRatio: number;
}

/**
 * Nudge `failing` (the color we may change) against the fixed `against` color
 * until the pair clears `targetRatio`, preserving hue by scaling toward black or
 * white (whichever direction the math requires). Returns null if even pure
 * black/white can't reach the target against `against` (e.g. against mid-grey).
 *
 * Which side to move: if `against` is light, darken `failing`; if dark, lighten.
 */
export function nudgeToClear(failing: Rgb, against: Rgb, targetRatio: number): ContrastFix | null {
    if (contrastRatio(failing, against) >= targetRatio) {
        return { newColor: toHex(failing), achievedRatio: contrastRatio(failing, against) };
    }
    // Move away from `against`'s luminance: toward black if the counterpart is
    // light, toward white if it's dark.
    const target: 0 | 255 = relativeLuminance(against) >= 0.5 ? 0 : 255;
    // Binary search the smallest t in (0,1] that clears the threshold. Evaluate the
    // ROUNDED hex color (not the float) so 8-bit quantization can't drop the final
    // result just under the threshold.
    // Round to 8-bit channels first, so the ratio we check matches the hex we emit
    // (float optimization could otherwise land just under threshold after rounding).
    const evalAt = (t: number): ContrastFix => {
        const f = scaleToward(failing, target, t);
        const rounded: Rgb = {
            r: Math.round(f.r),
            g: Math.round(f.g),
            b: Math.round(f.b)
        };
        return { newColor: toHex(rounded), achievedRatio: contrastRatio(rounded, against) };
    };
    // Unreachable even at the extreme → can't fix by nudging.
    if (evalAt(1).achievedRatio < targetRatio) {
        return null;
    }
    let lo = 0;
    let hi = 1;
    let best = evalAt(1);
    for (let i = 0; i < 24; i++) {
        const t = (lo + hi) / 2;
        const candidate = evalAt(t);
        if (candidate.achievedRatio >= targetRatio) {
            best = candidate;
            hi = t; // try a smaller nudge
        } else {
            lo = t; // need to move further
        }
    }
    return best;
}

/**
 * Color literals this module can `parseColor` — kept in lockstep with parseColor's
 * accepted syntax (#rgb / #rrggbb / rgb()/rgba()) so a token extracted here never
 * fails to parse. (Deliberately excludes 4/8-digit hex and hsl(), which parseColor
 * does not handle.)
 */
export const COLOR_TOKEN_RE = /#[0-9a-fA-F]{3}\b|#[0-9a-fA-F]{6}\b|rgba?\([^)]*\)/;

/** Find a parseable color token at/after `column` on the given 1-based source line. */
export function colorTokenAt(content: string, line: number, column: number): string | null {
    const lineText = content.split('\n')[line - 1];
    if (!lineText) {
        return null;
    }
    const fromCol = lineText.slice(Math.max(0, column));
    const m = fromCol.match(COLOR_TOKEN_RE) ?? lineText.match(COLOR_TOKEN_RE);
    return m ? m[0] : null;
}
