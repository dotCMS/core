/**
 *
 * Represents the configuration options for the DotCMS page editor.
 * @export
 * @interface DotCMSPageEditorConfig
 */
export interface DotCMSPageEditorConfig {
    /**
     * The pathname of the page being edited. Optional.
     */
    pathname: string;
    /**
     * The reload function to call when the page is reloaded.
     *
     * @deprecated
     */
    onReload?: () => void;
}
