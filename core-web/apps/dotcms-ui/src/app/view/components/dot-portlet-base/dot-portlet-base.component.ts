import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';
import { DotPortletToolbarComponent } from './components/dot-portlet-toolbar/dot-portlet-toolbar.component';

@Component({
    selector: 'dot-portlet-base',
    templateUrl: './dot-portlet-base.component.html',
    styleUrls: ['./dot-portlet-base.component.scss'],
    imports: [CommonModule, DotPortletBoxComponent, DotPortletToolbarComponent]
})
export class DotPortletBaseComponent {
    @Input()
    boxed = true;
}
