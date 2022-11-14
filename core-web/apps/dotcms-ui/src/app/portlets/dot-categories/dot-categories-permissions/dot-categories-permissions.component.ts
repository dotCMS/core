import { OnChanges, Component, Input, SimpleChanges } from '@angular/core';
@Component({
    selector: 'dot-categories-permissions',
    templateUrl: './dot-categories-permissions.component.html',
    styleUrls: ['./dot-categories-permissions.component.scss']
})
export class DotCategoriesPermissionsComponent implements OnChanges {
    @Input() categoryId: string;
    permissionsUrl = '';

    ngOnChanges(changes: SimpleChanges) {
        this.permissionsUrl = `/html/categories/permissions.jsp?categoryId=${changes.categoryId.currentValue}`;
    }
}
