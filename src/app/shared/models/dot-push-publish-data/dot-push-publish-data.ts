/**
 * Data required for push publish
 * @export
 * @interface DotPushPublishData
 */
export interface DotPushPublishData {
    pushActionSelected: string;
    publishdate: string;
    expiredate: string;
    environment: string[];
    forcePush: boolean;
    filterKey: string;
}
