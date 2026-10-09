/**
 * What the Edit Source panel is asked to open: one file's source, in a code editor.
 */
export interface DotSourceEditorRequest {
    /** The version whose source is loaded: the row's own, which is the working one. */
    inode: string;
    /** The file the save writes to, as a new working version. */
    identifier: string;
    /** Language of the version, so a multilingual file saves into the one that was opened. */
    languageId: number;
    /** Drawer header. */
    title: string;
    /** Monaco language id the source is highlighted with. */
    language: string;
}
