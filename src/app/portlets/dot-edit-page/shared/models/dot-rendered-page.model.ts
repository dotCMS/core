import { DotLayout } from './dot-layout.model';
import { DotPage } from './dot-page.model';
import { DotTemplate } from './dot-template.model';

export interface DotRenderedPage {
    html: string;
    layout: DotLayout;
    page: DotPage;
    containers?: any;
    template?: DotTemplate;
    canCreateTemplate: boolean;
}
