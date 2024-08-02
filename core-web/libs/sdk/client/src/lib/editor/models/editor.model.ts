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
     * @deprecated In future implementation we will be listening for the changes from the editor to update the page state so reload will not be needed.
     */
    onReload?: () => void;
}
