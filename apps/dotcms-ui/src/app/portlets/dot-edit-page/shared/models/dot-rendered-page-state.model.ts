import { User } from '@dotcms/dotcms-js';

import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotContainer } from '@shared/models/container/dot-container.model';
import { DotLayout, DotTemplate } from '@shared/models/dot-edit-layout-designer';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { DotPage } from '@models/dot-page/dot-page.model';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';

interface DotPageState {
    locked?: boolean;
    lockedByAnotherUser?: boolean;
    mode: DotPageMode;
}

export class DotPageRenderState extends DotPageRender {
    private _state: DotPageState;

    constructor(private _user: User, private dotRenderedPage: DotPageRender.Parameters) {
        super(dotRenderedPage);
        const locked = !!dotRenderedPage.page.lockedBy;
        const lockedByAnotherUser = locked ? dotRenderedPage.page.lockedBy !== _user.userId : false;

        this._state = {
            locked: locked,
            lockedByAnotherUser: lockedByAnotherUser,
            mode: dotRenderedPage.viewAs.mode
        };
    }

    get canCreateTemplate(): any {
        return this.dotRenderedPage.canCreateTemplate;
    }

    get containers(): {
        [key: string]: {
            container: DotContainer;
        };
    } {
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

    set dotRenderedPageState(dotRenderedPageState: DotPageRender) {
        this.dotRenderedPage = dotRenderedPageState;
    }
}
