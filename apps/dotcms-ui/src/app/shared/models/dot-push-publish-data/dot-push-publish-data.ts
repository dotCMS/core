/**
 * Data required for push publish
 * @export
 * @interface DotPushPublishData
 */
export interface DotPushPublishData {
    pushActionSelected: string;
    publishDate: string;
    expireDate: string;
    environment: string[];
    filterKey: string;
}
