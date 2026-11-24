import { DotCMSContentlet } from '@dotcms/dotcms-models';

/** The legacy editor asked to open a page in the page editor (the first save of a new page). */
export interface DotPageEditorRequest {
    url: string;
    languageId: number;
}

/**
 * What an editor panel reports to whoever opened it (#38303). Both editors produce the same set,
 * as `output()`s with these names, so a surface never needs to know which editor showed.
 */
export interface DotEditContentPanelEvents {
    /** Every successful save, and every restored version. Always the full saved contentlet. */
    saved: (contentlet: DotCMSContentlet) => void;

    /** The author switched language inside the editor; the panel stays open in the new language. */
    languageChanged: (languageId: number) => void;

    /** Close the panel and open the page editor at this URL and language. */
    pageEditorRequested: (request: DotPageEditorRequest) => void;

    /**
     * The content was removed from inside the editor. Always followed by `closed`. Never preceded
     * by `saved` for the delete.
     */
    deleted: () => void;

    /** The panel closed, for any reason. Emitted exactly once. */
    closed: () => void;
}

/** A surface listens to any subset of the panel events. */
export type DotEditContentPanelHandlers = Partial<DotEditContentPanelEvents>;
