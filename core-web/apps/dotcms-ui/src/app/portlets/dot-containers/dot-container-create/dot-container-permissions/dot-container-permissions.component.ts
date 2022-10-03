import { Component, OnInit } from '@angular/core';

@Component({
    selector: 'dot-container-permissions',
    templateUrl: './dot-container-permissions.component.html',
    styleUrls: ['./dot-container-permissions.component.scss']
})
export class DotContainerPermissionsComponent implements OnInit {
    permissionsUrl =  '/html/containers/permissions.jsp';
}
