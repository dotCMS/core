import { Component, OnChanges, OnInit } from '@angular/core';

@Component({
    selector: 'dot-container-permissions',
    templateUrl: './container-permissions.component.html',
    styleUrls: ['./container-permissions.component.scss']
})
export class ContainerPermissionsComponent implements OnInit, OnChanges {
    permissionsUrl = '';

    ngOnInit() {
        this.permissionsUrl = `/html/templates/permissions.jsp?templateId=7acdb856-4bbc-41c5-8695-a39c2e4a913f&popup=true&in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=templates`;
    }

    ngOnChanges(): void {}
}
