import { DotLayout, DotTemplate } from '@shared/models/dot-edit-layout-designer';
import { DotPage } from './dot-page.model';

export interface DotPageView {
    layout: DotLayout;
    page: DotPage;
    containers?: any;
    site?: any;
    template?: DotTemplate;
    canEditTemplate: boolean;
}
