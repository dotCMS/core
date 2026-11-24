/**
 * Centralized portlet URL constants.
 * Usage: Portlet.Content, Portlet.ContentTypes, etc.
 */
export const Portlet = {
    Content: '/dotAdmin/#/c/content',
    ContentTypes: '/dotAdmin/#/content-types-angular',
    ContentDrive: '/dotAdmin/#/content-drive',
    PublishingQueue: '/dotAdmin/#/c/publishing-queue',
    /** The Angular Publishing Queue; its bundles open their asset list in a dialog. */
    PublishingQueueBeta: '/dotAdmin/#/publishing-queue-beta',
    QueryTool: '/dotAdmin/#/query-tool',
    /** The legacy Workflow Tasks portlet; a task opens its detail at `<this>/<taskId>`. */
    WorkflowTasks: '/dotAdmin/#/c/workflow'
} as const;
