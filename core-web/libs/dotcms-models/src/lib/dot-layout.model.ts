import { DotLayoutBody } from './dot-layout-body.model';
import { DotLayoutSideBar } from './dot-layout-sidebar.model';

export interface DotLayout {
    body: DotLayoutBody;
    footer: boolean;
    header: boolean;
    /** Null when the layout has no sidebar; the template builder clears it so an empty
     *  sidebar is not persisted. */
    sidebar: DotLayoutSideBar | null;
    title: string;
    themeId?: string;
    width: string;
}

export interface DotTemplateDesigner {
    layout: DotLayout;
    /**
     * `null` means "save this as a page layout, not as a named template" — three call sites send it
     * deliberately, and the layout endpoint distinguishes it from an absent title.
     */
    title?: string | null;
    themeId: string;
}
