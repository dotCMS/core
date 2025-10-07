import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'dot-portlet-detail',
    templateUrl: './dot-portlet-detail.component.html',
    styleUrls: ['./dot-portlet-detail.component.scss'],
    standalone: false
})
export class DotPortletDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);

    isWorkflow = false;
    isContent = false;

    ngOnInit() {
        const currentPortlet: string = this.route.parent.parent.snapshot.params.id;
        this.isWorkflow = currentPortlet === 'workflow';
        this.isContent = !this.isWorkflow;
    }
}
