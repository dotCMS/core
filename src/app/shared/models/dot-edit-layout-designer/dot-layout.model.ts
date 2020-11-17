import { DotLayoutBody } from './dot-layout-body.model';
import { DotLayoutSideBar } from './dot-layout-sidebar.model';

export interface DotLayout {
    body: DotLayoutBody;
    footer: boolean;
    header: boolean;
    sidebar: DotLayoutSideBar;
    title: string;
    themeId?: string;
    width: string;
}
