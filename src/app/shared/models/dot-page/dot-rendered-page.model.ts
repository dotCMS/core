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
    constructor(private params: DotPageRender.Parameters) {}

    get layout(): DotLayout {
        return this.params.layout;
    }

    get page(): DotPage {
        return this.params.page;
    }

    get containers(): {
        [key: string]: {
            container: DotContainer;
        };
    } {
        return this.params.containers;
    }

    get template(): DotTemplate {
        return this.params.template;
    }

    get canCreateTemplate(): boolean {
        return this.params.canCreateTemplate;
    }

    get viewAs(): DotEditPageViewAs {
        return this.params.viewAs;
    }

    get numberContents(): number {
        return this.params.numberContents;
    }

    set numberContents(numberContents: number) {
        this.params.numberContents = numberContents;
    }
}
