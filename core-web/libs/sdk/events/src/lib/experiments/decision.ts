/**
 * What to do with an experiment's content on the current page, from what is stored:
 *
 * - `unknown`: nothing is stored about the experiment; dotCMS has to be asked.
 * - `excluded`: experiments are off, or the visitor was evaluated and is not in this one.
 * - `assigned`: the server rendered the visitor's variant.
 * - `ignored`: the visitor has another variant and the URL already asks for it, so the route
 *   does not pass `variantName` to its page request; loading the URL again would loop.
 * - `redirect`: the visitor has another variant, at `url`.
 */
export type VariantDecision =
    | { kind: 'unknown' }
    | { kind: 'excluded' }
    | { kind: 'assigned'; variant: string }
    | { kind: 'ignored'; variant: string }
    | { kind: 'redirect'; variant: string; url: string };

/** What `decideVariant` reads. */
export interface VariantDecisionInput {
    /** The experiment the page runs. */
    experimentId: string;
    /** The variant the server rendered. */
    rendered: string;
    /** The current URL. */
    href: string;
    /** The stored assignments, as parsed from storage: any shape is tolerated. */
    stored: unknown;
    /** Experiments are off (a 403 in the last day). */
    disabled: boolean;
    /** The current time, in ms. */
    now: number;
    /** The query parameter that selects a variant. */
    param: string;
    /** The name of the original variant. */
    defaultVariant: string;
}

/**
 * Decides an experiment's content from what is stored. It is pure, and the boot script prints
 * it with `Function.prototype.toString`, so it must not reference anything outside itself:
 * the engine and the boot script run this same function.
 *
 * @param input - The page's experiment, the URL, the stored assignments and the shared names
 * @returns What to do with the content
 */
export function decideVariant(input: VariantDecisionInput): VariantDecision {
    if (input.disabled) {
        return { kind: 'excluded' };
    }

    const stored = (input.stored || {}) as { experiments?: unknown; evaluatedIds?: unknown };
    const experiments = Array.isArray(stored.experiments)
        ? (stored.experiments as { id?: string; variant?: { name?: string }; expiresAt?: number }[])
        : [];
    const evaluated = Array.isArray(stored.evaluatedIds) ? (stored.evaluatedIds as string[]) : [];
    const assigned = experiments.find(
        (experiment) =>
            !!experiment &&
            experiment.id === input.experimentId &&
            (experiment.expiresAt || 0) > input.now
    );
    const variant = assigned && assigned.variant && assigned.variant.name;

    if (!variant) {
        return evaluated.indexOf(input.experimentId) >= 0
            ? { kind: 'excluded' }
            : { kind: 'unknown' };
    }

    if (variant === input.rendered) {
        return { kind: 'assigned', variant };
    }

    const url = new URL(input.href);
    const requested = variant === input.defaultVariant ? null : variant;

    if (url.searchParams.get(input.param) === requested) {
        return { kind: 'ignored', variant };
    }

    if (requested) {
        url.searchParams.set(input.param, requested);
    } else {
        url.searchParams.delete(input.param);
    }

    return { kind: 'redirect', variant, url: url.toString() };
}
