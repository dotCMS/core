import {
    DotContainer,
    DotContainerMap,
    DotContainerStructure,
    DotPageContainerStructure
} from './dot-container.model';
import { DotEditPageViewAs } from './dot-edit-page-view-as.model';
import { DotLayout } from './dot-layout.model';
import { DotPage } from './dot-page.model';
import { DotSite } from './dot-site.model';
import { DotTemplate } from './dot-template.model';

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
    urlContentMap?: { title: string; contentType?: string; inode?: string };
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

    get containers(): DotPageContainerStructure {
        return this._params.containers;
    }

    get containerMap(): DotContainerMap {
        return Object.keys(this.containers).reduce(
            (prev, curr) => ({
                ...prev,
                [curr]: this.containers[curr].container
            }),
            {}
        );
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
