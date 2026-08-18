/**
 * `jstat` ships no type declarations, and there is no `@types/jstat`.
 *
 * Only the beta distribution is declared, which is all `dot-experiment.utils` uses — it builds one
 * per bayesian variant to generate the probability-density curve. Kept deliberately narrow so a
 * change in what we use from the package is still a compile error.
 */
declare module 'jstat' {
    export const jStat: {
        beta: new (
            alpha: number,
            beta: number
        ) => {
            /** Probability density at `x`, for `x` in [0, 1]. */
            pdf(x: number): number;
        };
    };
}
