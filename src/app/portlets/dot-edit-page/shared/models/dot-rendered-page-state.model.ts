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
    private _state: DotPageState;

    constructor(private user: User, private dotRenderedPage: DotRenderedPage, state?: DotPageState) {
        this._state = state || this.getDefaultState(this.dotRenderedPage.page);
    }

    get canCreateTemplate(): any {
        return this.dotRenderedPage.canCreateTemplate;
    }

    get containers(): any {
        return this.dotRenderedPage.containers;
    }

    get html(): string {
        return this.dotRenderedPage.html;
    }

    get layout(): DotLayout {
        return this.dotRenderedPage.layout;
    }

    get page(): DotPage {
        return this.dotRenderedPage.page;
    }

    get state(): DotPageState {
        return this._state;
    }

    get template(): DotTemplate {
        return this.dotRenderedPage.template;
    }

    get viewAs(): DotEditPageViewAs {
        return this.dotRenderedPage.viewAs;
    }

    set dotRenderedPageState(dotRenderedPageState: DotRenderedPageState) {
        this.dotRenderedPage = dotRenderedPageState;
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
