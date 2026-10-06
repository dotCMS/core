import { NgTemplateOutlet } from '@angular/common';
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';

@Component({
    selector: 'dot-portlet-base',
    templateUrl: './dot-portlet-base.component.html',
    styleUrls: ['./dot-portlet-base.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotPortletBoxComponent, NgTemplateOutlet]
})
export class DotPortletBaseComponent {
    @Input()
    boxed = true;
}
