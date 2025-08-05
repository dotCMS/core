import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-portlet-base',
    templateUrl: './dot-portlet-base.component.html',
    styleUrls: ['./dot-portlet-base.component.scss'],
    standalone: false
})
export class DotPortletBaseComponent {
    @Input()
    boxed = true;
}
