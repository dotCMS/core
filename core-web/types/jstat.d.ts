/**
 * `jstat` ships no type declarations, and there is no `@types/jstat`.
 *
 * This is wired up through `paths` in `tsconfig.base.json` rather than as an ambient
 * `declare module` inside the one library that imports it. An ambient declaration is only visible
 * to the project whose `include` reaches it, so a strict consumer compiling that library's sources
 * through a path mapping saw the import as an implicit `any` — `edit-ema/portlet` did, while
 * `dot-experiments` itself measured 0. A `paths` entry is inherited by every project extending this
 * config, so every consumer resolves the same declaration. `paths` affects type resolution only;
 * the bundler still resolves the real package at runtime.
 *
 * Only the beta distribution is declared, which is all `dot-experiment.utils` uses — it builds one
 * per bayesian variant to generate the probability-density curve. Kept deliberately narrow so a
 * change in what we use from the package is still a compile error.
 */
export const jStat: {
    beta: new (
        alpha: number,
        beta: number
    ) => {
        /** Probability density at `x`, for `x` in [0, 1]. */
        pdf(x: number): number;
    };
};
