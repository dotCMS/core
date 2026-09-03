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
    /**
     * Called once the push has been accepted, so the caller can say so.
     *
     * The dialog signals success only by closing, which reads as it having given up rather than
     * having worked. Rather than give a globally-opened dialog a messaging concern — and a
     * `MessageService` it cannot rely on being provided — the caller passes what to do and owns its
     * own copy. Optional: consumers that want the current silent close simply omit it.
     */
    onSuccess?: () => void;
}
