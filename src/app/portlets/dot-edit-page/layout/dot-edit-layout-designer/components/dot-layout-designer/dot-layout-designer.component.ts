import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DotEditLayoutGridComponent } from '../../../components/dot-edit-layout-grid/dot-edit-layout-grid.component';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    selector: 'dot-layout-designer',
    templateUrl: './dot-layout-designer.component.html',
    styleUrls: ['./dot-layout-designer.component.scss']
})
export class DotLayoutDesignerComponent implements OnInit {
    @ViewChild('editLayoutGrid')
    editLayoutGrid: DotEditLayoutGridComponent;

    @Input()
    group: FormGroup;

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['editpage.layout.designer.header', 'editpage.layout.designer.footer'])
            .subscribe();
    }

    /**
     * Add a grid box to the ng grid layout component
     *
     * @memberof DotLayoutDesignerComponent
     */
    addGridBox(): void {
        this.editLayoutGrid.addBox();
    }
}
