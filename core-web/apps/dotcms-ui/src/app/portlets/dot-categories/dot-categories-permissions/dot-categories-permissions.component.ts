import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { IframeComponent } from '../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@Component({
    selector: 'dot-categories-permissions',
    templateUrl: './dot-categories-permissions.component.html',
    styleUrls: ['./dot-categories-permissions.component.scss'],
    imports: [DotPortletBaseComponent, IframeComponent]
})
export class DotCategoriesPermissionsComponent implements OnChanges {
    @Input() categoryId: string;
    permissionsUrl = '';

    ngOnChanges(changes: SimpleChanges) {
        this.permissionsUrl = `/html/categories/permissions.jsp?categoryId=${changes.categoryId.currentValue}`;
    }
}
