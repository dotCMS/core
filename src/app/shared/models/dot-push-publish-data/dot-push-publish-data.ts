/**
 * Data required for push publish
 * @export
 * @interface DotPushPublishData
 */
export interface DotPushPublishData {
    pushActionSelected: string;
    publishdate: string;
    publishdatetime: string;
    expiredate: string;
    expiredatetime: string;
    environment: string[];
    forcePush: boolean;
    filterKey: string;
}
