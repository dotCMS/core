import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DotEditLayoutGridComponent } from '../../../components/dot-edit-layout-grid/dot-edit-layout-grid.component';
import { DotMessageService } from '@services/dot-messages-service';
import { Observable } from 'rxjs';

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

    messages$: Observable<{ [key: string]: string }>;

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.messages$ = this.dotMessageService.getMessages([
            'editpage.layout.designer.header',
            'editpage.layout.designer.footer'
        ]);
    }
}
