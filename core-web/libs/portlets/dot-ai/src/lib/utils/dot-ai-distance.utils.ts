import { DOT_AI_VECTOR_OPERATOR } from '@dotcms/dotcms-models';

/**
 * The request names the operator (`innerProduct`); the **response echoes the pgvector symbol**
 * (`<#>`). Both have to be recognised here, because the bar is drawn from the response.
 */
const INNER_PRODUCT_FORMS: ReadonlySet<string> = new Set([
    DOT_AI_VECTOR_OPERATOR.INNER_PRODUCT,
    '<#>'
]);

/**
 * Turns a raw vector distance into a 0–100 "closeness" value for the result bar.
 *
 * The three operators the backend supports do not share a scale, and they do not even agree on
 * which direction is better:
 *
 * - `cosine` (`<=>`) runs 0..2, and `distance` (`<->`) is unbounded L2. For both, a *smaller*
 *   number is a closer match, so the magnitude is inverted.
 * - `innerProduct` (`<#>`) returns the **negated** inner product, so a *more negative* number
 *   is a closer match — around `-0.65` for the best hit measured against a live index.
 *
 * That last one is why the operator has to be passed in. Inverting the magnitude for every
 * operator ranked inner-product results exactly backwards: the best match (`-0.65`) drew the
 * emptiest bar and the worst (`-0.09`) drew the fullest.
 *
 * The raw distance is still shown beside the bar, so nothing is hidden by the normalisation.
 */
export function toClosenessPercent(distance: number, operator: string): number {
    if (!Number.isFinite(distance)) {
        return 0;
    }

    if (INNER_PRODUCT_FORMS.has(operator)) {
        // Similarity is how far below zero it sits; a positive value here means dissimilar.
        return Math.round(clamp(-distance) * 100);
    }

    return Math.round((1 - clamp(Math.abs(distance))) * 100);
}

const clamp = (value: number): number => Math.min(Math.max(value, 0), 1);
