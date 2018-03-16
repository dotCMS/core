import { DotRenderedPage } from './dot-rendered-page.model';
import { PageMode } from './page-mode.enum';
import { User } from 'dotcms-js/dotcms-js';
import { DotPage } from './dot-page.model';
import { DotLayout } from './dot-layout.model';
import { DotTemplate } from './dot-template.model';
import { DotEditPageViewAs } from '../../../../shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';

export interface DotPageState {
    locked: boolean;
    lockedByAnotherUser?: boolean;
    mode: PageMode;
}

export class DotRenderedPageState {
    private _canCreateTemplate: boolean;
    private _containers?: any;
    private _html: string;
    private _layout: DotLayout;
    private _page: DotPage;
    private _state: DotPageState;
    private _template?: DotTemplate;
    private _viewAs?: DotEditPageViewAs;

    constructor(dotRenderedPage: DotRenderedPage, state: DotPageState, private user: User) {
        this._page = dotRenderedPage.page;
        this._html = dotRenderedPage.html;
        this._containers = dotRenderedPage.containers;
        this._layout = dotRenderedPage.layout;
        this._template = dotRenderedPage.template;
        this._state = state || this.getDefaultState(this._page);
        this._viewAs = dotRenderedPage.viewAs;
        this._canCreateTemplate = dotRenderedPage.canCreateTemplate;
    }

    get canCreateTemplate(): any {
        return this._canCreateTemplate;
    }

    get containers(): any {
        return this._containers;
    }

    get html(): string {
        return this._html;
    }

    get layout(): DotLayout {
        return this._layout;
    }

    get page(): DotPage {
        return this._page;
    }

    get state(): DotPageState {
        return this._state;
    }

    get template(): DotTemplate {
        return this._template;
    }

    get viewAs(): DotEditPageViewAs {
        return this._viewAs;
    }

    private getDefaultState(page: DotPage): DotPageState {
        const locked = !!page.lockedBy;
        const lockedByAnotherUser = locked ? page.lockedBy !== this.user.userId : false;
        const mode: PageMode = lockedByAnotherUser ? PageMode.PREVIEW : this.getPageMode(page, locked);

        return {
            locked,
            lockedByAnotherUser,
            mode
        };
    }

    private getPageMode(page: DotPage, locked: boolean): PageMode {
        return locked && page.canLock ? PageMode.EDIT : PageMode.PREVIEW;
    }
}
