import { DotPage } from './dot-page.model';
import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotContainer, DotContainerStructure } from '@shared/models/container/dot-container.model';
import { DotLayout, DotTemplate } from '@shared/models/dot-edit-layout-designer';
import { DotSite } from '@dotcms/app/portlets/dot-edit-page/shared/models';

export interface DotPageRenderParameters {
    layout?: DotLayout;
    page: DotPage;
    containers?: {
        [key: string]: {
            container: DotContainer;
            containerStructures?: DotContainerStructure[];
        };
    };
    template?: DotTemplate;
    site?: DotSite;
    canCreateTemplate: boolean;
    viewAs: DotEditPageViewAs;
    numberContents: number;
}

export class DotPageRender {
    constructor(private _params: DotPageRenderParameters) {}

    get params(): DotPageRenderParameters {
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

    get site(): DotSite {
        return this._params.site;
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
