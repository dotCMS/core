import { DotMessageService } from '@services/dot-messages-service';
import { FormGroup } from '@angular/forms';
import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';

@Component({
    selector: 'dot-layout-properties',
    templateUrl: './dot-layout-properties.component.html',
    styleUrls: ['./dot-layout-properties.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class DotLayoutPropertiesComponent implements OnInit {
    @Input()
    group: FormGroup;

    messages: { [key: string]: string } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['editpage.layout.properties.header', 'editpage.layout.properties.footer'])
            .subscribe((messages: { [key: string]: string }) => {
                this.messages = messages;
            });
    }
}
