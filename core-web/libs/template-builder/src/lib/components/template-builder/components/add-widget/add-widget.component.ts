import { DDElementHost } from 'gridstack/dist/dd-element';

import { NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, Input } from '@angular/core';

@Component({
    selector: 'dotcms-add-widget',
    templateUrl: './add-widget.component.html',
    styleUrls: ['./add-widget.component.scss'],
    standalone: true,
    imports: [NgIf],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddWidgetComponent {
    @Input() label = 'Add Widget';
    @Input() icon = '';

    protected imageError = false;

    constructor(private el: ElementRef) {}

    get nativeElement(): DDElementHost {
        return this.el.nativeElement;
    }
}
