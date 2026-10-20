/**
 * What the Edit Code panel is asked to open: one file's source, in a code editor.
 */
export interface DotSourceEditorRequest {
    /**
     * The version whose source is loaded: the row's own, which is the working one. The save is fired
     * on it, so it also fixes the file and the language the save goes to.
     */
    inode: string;
    /** Drawer header. */
    title: string;
    /** Monaco language id the source is highlighted with. */
    language: string;
}
