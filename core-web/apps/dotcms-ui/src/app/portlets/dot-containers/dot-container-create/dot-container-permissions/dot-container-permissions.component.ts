import { Component, Input, OnInit } from '@angular/core';

@Component({
    selector: 'dot-container-permissions',
    templateUrl: './dot-container-permissions.component.html',
    styleUrls: ['./dot-container-permissions.component.scss'],
    standalone: false
})
export class DotContainerPermissionsComponent implements OnInit {
    @Input() containerId: string;
    permissionsUrl = '/html/containers/permissions.jsp';
    ngOnInit() {
        this.permissionsUrl = `/html/containers/permissions.jsp?containerId=${this.containerId}&popup=true`;
    }
}
