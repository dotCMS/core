/**
 * Data required for push publish
 * @export
 * @interface PushPublishData
 */
export interface PushPublishData {
    pushActionSelected: string;
    publishdate: string;
    publishdatetime: string;
    expiredate: string;
    expiredatetime: string;
    environment: string[];
    forcePush: boolean;
    filterKey: string;
}

export interface DotPushPublishEvent {
    assetIdentifier: string;
    dateFilter?: boolean;
    removeOnly?: boolean;
    isBundle?: boolean;
}
