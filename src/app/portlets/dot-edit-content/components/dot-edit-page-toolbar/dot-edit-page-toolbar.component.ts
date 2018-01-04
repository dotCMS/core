import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit {
    @Input() pageTitle: string;
    @Output() save = new EventEmitter<MouseEvent>();
    @Output() cancel = new EventEmitter<MouseEvent>();

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService.getMessages([
            'editpage.toolbar.primary.action',
            'editpage.toolbar.secondary.action',
        ]).subscribe();
    }
}
