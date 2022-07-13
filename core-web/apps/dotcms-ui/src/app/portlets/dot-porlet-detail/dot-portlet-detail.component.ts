import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'dot-portlet-detail',
    templateUrl: './dot-portlet-detail.component.html',
    styleUrls: ['./dot-portlet-detail.component.scss']
})
export class DotPortletDetailComponent implements OnInit {
    isWorkflow = false;
    isContent = false;
    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        const currentPortlet: string = this.route.parent.parent.snapshot.params.id;
        this.isWorkflow = currentPortlet === 'workflow';
        this.isContent = !this.isWorkflow;
    }
}
