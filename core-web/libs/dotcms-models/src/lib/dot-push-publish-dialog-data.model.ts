/**
 * Data that came from legacy PushPublish Dialog
 * @export
 * @interface DotPushPublishDialogData
 */
export interface DotPushPublishDialogData {
    assetIdentifier: string;
    title: string;
    dateFilter?: boolean;
    removeOnly?: boolean;
    isBundle?: boolean;
    restricted?: boolean;
    cats?: boolean;
    customCode?: string;
}
