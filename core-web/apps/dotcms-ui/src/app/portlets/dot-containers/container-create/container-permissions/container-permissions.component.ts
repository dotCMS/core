import { Component, OnInit } from '@angular/core';

@Component({
    selector: 'dot-container-permissions',
    templateUrl: './container-permissions.component.html',
    styleUrls: ['./container-permissions.component.scss']
})
export class ContainerPermissionsComponent implements OnInit {
    permissionsUrl = '';

    ngOnInit() {
        this.permissionsUrl = `/html/containers/permissions.jsp`;
    }
}
