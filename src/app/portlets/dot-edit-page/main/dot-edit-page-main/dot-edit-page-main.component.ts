import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pipe, Observable } from 'rxjs/Rx';
import { pluck, map } from 'rxjs/operators';

import { DotPageView } from '../../shared/models/dot-page-view.model';

export const getDrawed = pluck('pageView', 'template', 'drawed');
export const isAdvanced = map((isDrawed: boolean) => !isDrawed);
export const getTemplateType = pipe(
    getDrawed,
    isAdvanced
);

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit {
    isAdvancedTemplate: Observable<boolean>;

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.isAdvancedTemplate = this.route.data.let(getTemplateType);
    }
}
