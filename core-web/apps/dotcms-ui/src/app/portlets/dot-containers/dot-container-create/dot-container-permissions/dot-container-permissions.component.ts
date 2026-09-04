import { Component, OnInit, ChangeDetectionStrategy, input } from '@angular/core';

import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';

@Component({
    selector: 'dot-container-permissions',
    templateUrl: './dot-container-permissions.component.html',
    styleUrls: ['./dot-container-permissions.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotPortletBoxComponent, IframeComponent]
})
export class DotContainerPermissionsComponent implements OnInit {
    readonly containerId = input<string>();
    permissionsUrl = '/html/containers/permissions.jsp';
    ngOnInit() {
        this.permissionsUrl = `/html/containers/permissions.jsp?containerId=${this.containerId()}&popup=true`;
    }
}
