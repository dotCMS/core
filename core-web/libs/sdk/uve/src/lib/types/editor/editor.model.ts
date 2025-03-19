/**
 * @description Custom client parameters for fetching data.
 */
export type DotCMSClientParams = {
    depth: string;
};

/**
 * Configuration for reordering a menu.
 */
export interface DotCMSReorderMenuConfig {
    /**
     * The starting level of the menu to be reordered.
     */
    startLevel: number;

    /**
     * The depth of the menu levels to be reordered.
     */
    depth: number;
}
