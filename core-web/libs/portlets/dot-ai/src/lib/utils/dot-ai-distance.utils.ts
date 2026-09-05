/**
 * Turns a raw vector distance into a 0–100 "closeness" value for the result bar.
 *
 * The three operators the backend supports do not share a scale, and one of them is negative:
 *
 * - `innerProduct` (`<#>`) returns **negative** values — around `-0.33` measured against a
 *   live index. A bar bound straight to that renders empty.
 * - `cosine` (`<=>`) runs 0..2.
 * - `distance` (`<->`) is unbounded L2.
 *
 * Since the user can switch operator from the settings panel, the bar has to stay meaningful
 * across all three. Taking the magnitude and inverting it means "closer" always reads as a
 * fuller bar, whichever operator produced the number. The raw distance is still shown beside
 * it, so nothing is hidden by the normalisation.
 */
export function toClosenessPercent(distance: number): number {
    if (!Number.isFinite(distance)) {
        return 0;
    }

    const magnitude = Math.min(Math.abs(distance), 1);

    return Math.round((1 - magnitude) * 100);
}
