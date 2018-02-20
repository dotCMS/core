import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { FormGroup } from '@angular/forms';

// TODO: Implement ControlValueAccessor instead of passing the formGroup
@Component({
    selector: 'dot-sidebar-properties',
    templateUrl: './dot-sidebar-properties.component.html'
})
export class DotSidebarPropertiesComponent implements OnInit {
    @Input() group: FormGroup;
    @ViewChild('overlay') overlay: any;

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'editpage.layout.sidebar.width.small',
                'editpage.layout.sidebar.width.medium',
                'editpage.layout.sidebar.width.large',
                'editpage.layout.sidebar.action.open'
            ])
            .subscribe();
    }
}
