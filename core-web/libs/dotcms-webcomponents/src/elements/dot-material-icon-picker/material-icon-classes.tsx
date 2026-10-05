/**
 * The picker's icon list — the canonical source now lives in
 * `@dotcms/dotcms-models` (`DOT_MATERIAL_ICONS`) so the Tools portlet picks
 * from the same set without maintaining a second ~1.7k-line copy. The
 * `MaterialIconClasses` export is kept as a re-export so this picker's
 * existing import path (`./material-icon-classes`) stays stable.
 *
 * eslint-disable: the `enforce-module-boundaries` rule prevents a buildable
 * library (`dotcms-webcomponents`) from importing a non-buildable one
 * (`dotcms-models`). Making models buildable just for this one re-export
 * would be a bigger change than the problem calls for, and the import is
 * strict data — no runtime, no Angular, no Stencil — so the boundary risk
 * the rule guards against does not apply here.
 */
// eslint-disable-next-line @nx/enforce-module-boundaries
export { DOT_MATERIAL_ICONS as MaterialIconClasses } from '@dotcms/dotcms-models';
