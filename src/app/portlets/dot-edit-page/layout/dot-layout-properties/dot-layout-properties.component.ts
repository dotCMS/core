import { MessageService } from '../../../../api/services/messages-service';
import { FormGroup } from '@angular/forms';
import { Component, ViewEncapsulation, Input, OnInit } from '@angular/core';

@Component({
    selector: 'dot-layout-properties',
    templateUrl: './dot-layout-properties.component.html',
    styleUrls: ['./dot-layout-properties.component.scss']
})
export class DotLayoutPropertiesComponent implements OnInit {
    @Input() group: FormGroup;

    constructor(public messageService: MessageService) {}

    ngOnInit() {
        this.messageService.getMessages([
            'editpage.layout.properties.header',
            'editpage.layout.properties.footer',
        ]).subscribe();
    }
}




