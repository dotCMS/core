/**
 *
 * Represents the configuration options for the DotCMS page editor.
 * @export
 * @interface DotCMSPageEditorConfig
 */
export interface DotCMSPageEditorConfig {
    /**
     * A callback function that will be called when the page editor needs to be reloaded.
     */
    onReload: () => void;

    /**
     * The pathname of the page being edited. Optional.
     */
    pathname?: string;
}
