interface DotCMSPageEditorConfig {
    onReload: () => void;
}
declare class DotCMSPageEditor {
    private config;
    private subscriptions;
    isInsideEditor: boolean;
    constructor(config?: DotCMSPageEditorConfig);
    init(): void;
    destroy(): void;
    updateNavigation(pathname: string): void;
    private listenEditorMessages;
    private listenHoveredContentlet;
    private scrollHandler;
    private checkIfInsideEditor;
    private listenContentChange;
    private setBounds;
    private reloadPage;
}
export declare const sdkDotPageEditor: {
    createClient: (config?: DotCMSPageEditorConfig) => DotCMSPageEditor;
};
export {};
