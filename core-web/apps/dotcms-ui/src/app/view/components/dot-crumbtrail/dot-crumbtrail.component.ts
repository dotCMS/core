import { Observable } from 'rxjs';

import { Component, OnInit } from '@angular/core';

import { DotCrumb, DotCrumbtrailService } from './service/dot-crumbtrail.service';
@Component({
    selector: 'dot-crumbtrail',
    template: '<p-breadcrumb [model]="crumb | async" />',
    styleUrls: ['./dot-crumbtrail.component.scss']
})
export class DotCrumbtrailComponent implements OnInit {
    crumb: Observable<DotCrumb[]>;

    constructor(private crumbTrailService: DotCrumbtrailService) {}

    ngOnInit() {
        this.crumb = this.crumbTrailService.crumbTrail$;
    }
}
