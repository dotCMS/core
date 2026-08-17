/**
 * Data required for push publish
 * @export
 * @interface DotPushPublishData
 */
export interface DotPushPublishData {
    pushActionSelected: string;
    publishDate?: string;
    expireDate?: string;
    environment: string[];
    filterKey?: string;
    timezoneId: string;
}

/** What the user can ask a push publish for. Values are the backend's `iWantTo` vocabulary. */
export type DotWorkflowPushPublishAction = 'publish' | 'expire' | 'publishexpire';

/**
 * The push publish payload, in the shape the backend's `PushPublishBean` expects.
 *
 * Already converted — dates split into `yyyy-MM-dd` + `HH-mm` pairs, environments comma-joined into
 * `whereToSend` — so a consumer can hand it straight to a fire request without repeating the
 * transformation that `DotWorkflowEventHandlerService.processWorkflowPayload` does today.
 *
 * Lives here rather than beside `DotWorkflowPushPublishComponent` because both the component that
 * produces it (`@dotcms/ui`) and the service that posts it (`@dotcms/data-access`) need it, and
 * data-access must not depend on ui.
 */
export interface DotWorkflowPushPublishValue {
    /** Comma-joined environment ids. */
    whereToSend: string;
    iWantTo: DotWorkflowPushPublishAction;
    publishDate: string;
    publishTime: string;
    expireDate: string;
    expireTime: string;
    filterKey: string;
    timezoneId: string;
}
