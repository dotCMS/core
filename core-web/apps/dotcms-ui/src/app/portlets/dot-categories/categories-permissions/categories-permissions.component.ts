import { OnInit, OnChanges, Component, Input, SimpleChanges } from '@angular/core';
@Component({
    selector: 'dot-categories-permissions',
    templateUrl: './categories-permissions.component.html',
    styleUrls: ['./categories-permissions.component.scss']
})
export class CategoriesPermissionsComponent implements OnInit, OnChanges {
    @Input() categoryId: string;
    permissionsUrl = '';

    ngOnInit() {
        this.permissionsUrl = `/html/categories/permissions.jsp?categoryId=${this.categoryId}`;
    }

    ngOnChanges(changes: SimpleChanges) {
        this.permissionsUrl = `/html/categories/permissions.jsp?categoryId=${changes.categoryId.currentValue}`;
    }
}
