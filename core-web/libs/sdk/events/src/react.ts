/**
 * `@dotcms/events/react`: `DotCMSExperiment`, which prints the markup for React. The only
 * entry that imports React. It brings the markup and never the SDK, which loads with
 * `events.init`.
 */

export { DotCMSExperiment } from './lib/react/DotCMSExperiment';
export type { DotCMSExperimentProps } from './lib/react/DotCMSExperiment';
export type { DotCMSEventsExperimentPage } from './lib/models';
