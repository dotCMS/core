import { DotPage } from './dot-page.model';
import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotContainer } from '@shared/models/container/dot-container.model';
import { DotLayout, DotTemplate } from '@shared/models/dot-edit-layout-designer';

export module DotPageRender {
    export interface Parameters {
        layout?: DotLayout;
        page: DotPage;
        containers?: {
            [key: string]: {
                container: DotContainer;
            };
        };
        template?: DotTemplate;
        canCreateTemplate: boolean;
        viewAs: DotEditPageViewAs;
        numberContents: number;
    }
}

export class DotPageRender {
    constructor(private _params: DotPageRender.Parameters) {}

    get params(): DotPageRender.Parameters {
        return this._params;
    }

    get layout(): DotLayout {
        return this._params.layout;
    }

    get page(): DotPage {
        return this._params.page;
    }

    get containers(): {
        [key: string]: {
            container: DotContainer;
        };
    } {
        return this._params.containers;
    }

    get template(): DotTemplate {
        return this._params.template;
    }

    get canCreateTemplate(): boolean {
        return this._params.canCreateTemplate;
    }

    get viewAs(): DotEditPageViewAs {
        return this._params.viewAs;
    }

    get numberContents(): number {
        return this._params.numberContents;
    }

    set numberContents(numberContents: number) {
        this._params.numberContents = numberContents;
    }
}
