/**
 * `@dotcms/events/markup`: what a page prints so its experiment runs, for any framework that
 * renders HTML on the server. It has its own entry so the SDK, which every page loads, never
 * references the boot script builder.
 */

export { experimentMarkup } from './lib/experiments/markup';
export type { DotCMSEventsExperimentMarkup, DotCMSEventsExperimentPage } from './lib/models';
