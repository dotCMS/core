import { User } from '@dotcms/dotcms-js';
import { DotPageContainerStructure } from './dot-container.model';
import { DotCMSContentlet } from './dot-contentlet.model';
import { DotEditPageViewAs } from './dot-edit-page-view-as.model';
import { DotLayout } from './dot-layout.model';
import { DotPageMode } from './dot-page-mode.enum';
import { DotPage } from './dot-page.model';
import { DotPageRender, DotPageRenderParameters } from './dot-rendered-page.model';
import { DotTemplate } from './dot-template.model';

export interface DotPageState {
    favoritePage?: DotCMSContentlet;
    locked?: boolean;
    lockedByAnotherUser?: boolean;
    mode: DotPageMode;
}

export class DotPageRenderState extends DotPageRender {
    private _state: DotPageState;

    constructor(
        private _user: User,
        private dotRenderedPage: DotPageRenderParameters,
        _favoritePage?: DotCMSContentlet
    ) {
        super(dotRenderedPage);
        const locked = !!dotRenderedPage.page.lockedBy;
        const lockedByAnotherUser = locked ? dotRenderedPage.page.lockedBy !== _user.userId : false;

        this._state = {
            favoritePage: _favoritePage,
            locked: locked,
            lockedByAnotherUser: lockedByAnotherUser,
            mode: dotRenderedPage.viewAs.mode
        };
    }

    get canCreateTemplate(): boolean {
        return this.dotRenderedPage.canCreateTemplate;
    }

    get containers(): DotPageContainerStructure {
        return this.dotRenderedPage.containers;
    }

    get html(): string {
        return this.dotRenderedPage.page.rendered;
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

    get user(): User {
        return this._user;
    }

    get favoritePage(): DotCMSContentlet {
        return this._state.favoritePage;
    }

    set favoritePage(favoritePage: DotCMSContentlet) {
        this._state.favoritePage = favoritePage;
    }

    set dotRenderedPageState(dotRenderedPageState: DotPageRender) {
        this.dotRenderedPage = dotRenderedPageState;
    }
}
