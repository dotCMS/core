import { Component, Input, OnInit } from '@angular/core';

@Component({
    selector: 'dot-portlet-base',
    templateUrl: './dot-portlet-base.component.html',
    styleUrls: ['./dot-portlet-base.component.scss']
})
export class DotPortletBaseComponent implements OnInit {
    @Input()
    boxed = true;

    constructor() {}

    ngOnInit(): void {}
}
