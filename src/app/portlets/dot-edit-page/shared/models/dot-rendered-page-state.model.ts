import { DotRenderedPage } from './dot-rendered-page.model';
import { PageMode } from './page-mode.enum';
import { User } from 'dotcms-js/dotcms-js';

export interface DotPageState {
    locked: boolean;
    lockedByAnotherUser?: boolean;
    mode: PageMode;
}

export class DotRenderedPageState {
    private _page: DotRenderedPage;
    private _state: DotPageState;

    constructor(page: DotRenderedPage, state: DotPageState, private user: User) {
        this._page = page;
        this._state = state || this.getDefaultState(this._page);
    }

    get page(): DotRenderedPage {
        return this._page;
    }

    get state(): DotPageState {
        return this._state;
    }

    private getDefaultState(page: DotRenderedPage): DotPageState {
        const locked = !!page.lockedBy;
        const lockedByAnotherUser = locked ? page.lockedBy !== this.user.userId : false;
        const mode: PageMode = lockedByAnotherUser ? PageMode.PREVIEW : this.getPageMode(page, locked);

        return {
            locked,
            lockedByAnotherUser,
            mode
        };
    }

    private getPageMode(page: DotRenderedPage, locked: boolean): PageMode {
        return locked && page.canLock ? PageMode.EDIT : PageMode.PREVIEW;
    }
}
