import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
@Component({
    selector: 'dot-categories-permissions',
    templateUrl: './dot-categories-permissions.component.html',
    styleUrls: ['./dot-categories-permissions.component.scss'],
    standalone: false
})
export class DotCategoriesPermissionsComponent implements OnChanges {
    @Input() categoryId: string;
    permissionsUrl = '';

    ngOnChanges(changes: SimpleChanges) {
        this.permissionsUrl = `/html/categories/permissions.jsp?categoryId=${changes.categoryId.currentValue}`;
    }
}
