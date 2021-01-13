import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotTemplateItem } from '../store/dot-template.store';

@Component({
    selector: 'dot-template-builder',
    templateUrl: './dot-template-builder.component.html',
    styleUrls: ['./dot-template-builder.component.scss']
})
export class DotTemplateBuilderComponent implements OnInit {
    @Input() item: DotTemplateItem;
    @Output() save = new EventEmitter<DotTemplateItem>();
    @Output() cancel = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
    permissionsUrl = '';
    historyUrl = '';

    constructor() {}

    ngOnInit() {
        this.permissionsUrl = `/html/templates/permissions.jsp?templateId=${this.item.identifier}&popup=true`;
        this.historyUrl = `/html/templates/push_history.jsp?templateId=${this.item.identifier}&popup=true`;
    }
}
