import { DotLayout } from './dot-layout.model';
import { DotPage } from './dot-page.model';

export interface DotPageView {
    layout: DotLayout;
    page: DotPage;
    containers?: any;
    site?: any;
    template?: any;
}
