import { Component } from '@angular/core';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-dot-template-selector',
    templateUrl: './dot-template-selector.component.html',
    styleUrls: ['./dot-template-selector.component.scss']
})
export class DotTemplateSelectorComponent {
    value = 'designer';

    map = {
        designer: this.dotMessageService.get('templates.template.selector.design'),
        advanced: this.dotMessageService.get('templates.template.selector.advanced')
    };

    constructor(private dotMessageService: DotMessageService, private ref: DynamicDialogRef) {}

    onClick(): void {
        this.ref.close(this.value);
    }
}
