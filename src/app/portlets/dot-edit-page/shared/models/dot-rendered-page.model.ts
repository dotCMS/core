import { DotLayout } from './dot-layout.model';
import { DotPage } from './dot-page.model';
import { DotTemplate } from './dot-template.model';
import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';

export module DotPageRender {
    export interface Parameters {
        layout?: DotLayout;
        page: DotPage;
        containers?: any;
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

    get containers(): any {
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
