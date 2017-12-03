import { DotLayout } from './dot-layout.model';
import { DotPage } from './dot-page.model';
import { DotTemplate } from './dot-template.model';

export interface DotPageView {
    layout: DotLayout;
    page: DotPage;
    containers?: any;
    site?: any;
    template?: DotTemplate;
}
