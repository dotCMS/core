import { Component, input } from '@angular/core';

@Component({
    selector: 'dot-portlet-base',
    templateUrl: './dot-portlet-base.component.html',
    styleUrls: ['./dot-portlet-base.component.scss']
})
export class DotPortletBaseComponent {
    $boxed = input<boolean>(true, { alias: 'boxed' });
}
