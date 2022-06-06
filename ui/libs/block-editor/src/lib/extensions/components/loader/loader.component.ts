import { Component, ViewEncapsulation } from '@angular/core';
import { AngularNodeViewComponent } from '../../../NodeViewRenderer';

export const enum MessageType {
    INFO = 'info',
    ERROR = 'error'
}

@Component({
    selector: 'dotcms-message',
    templateUrl: './loader.component.html',
    styleUrls: ['./loader.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class LoaderComponent extends AngularNodeViewComponent {
    data: {
        type: MessageType;
        message: string;
    };
}
