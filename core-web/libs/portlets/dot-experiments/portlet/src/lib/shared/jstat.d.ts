/**
 * `jstat` ships no type declarations, and there is no `@types/jstat`.
 *
 * An ambient `declare module` rather than a `paths` entry in `tsconfig.base.json`: a `paths` entry
 * points the module id at this `.d.ts`, and Vite resolves the same mapping at runtime, so the
 * import would evaluate to an empty module under Vitest and in a Vite build. The triple-slash
 * reference in `dot-experiment-results.utils.ts` pulls this file into the program of every project
 * that compiles those sources, including consumers reaching them through a path mapping.
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
