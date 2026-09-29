/**
 * `@dotcms/events`: the `events` object and the types its methods and options use.
 *
 * Every page loads this entry, so it holds the SDK and no markup: what a page prints so its
 * experiment runs comes from `./markup` and `./react`.
 */

export { events } from './lib/events';
export type {
    DotCMSEvents,
    DotCMSEventsConfig,
    DotCMSEventsError,
    DotCMSEventsErrorCode,
    DotCMSEventsExperimentsConfig,
    DotCMSEventsImpressionsConfig,
    DotCMSEventsJsonObject,
    DotCMSEventsJsonValue,
    DotCMSEventsLogLevel,
    DotCMSEventsQueueConfig
} from './lib/models';
