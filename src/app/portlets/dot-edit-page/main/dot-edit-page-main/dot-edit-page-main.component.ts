import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { pluck, map } from 'rxjs/operators';
// tslint:disable-next-line:import-blacklist
import { pipe } from 'rxjs';

export const getDrawed = pluck('pageView', 'template', 'drawed');
export const isAdvanced = map((isDrawed: boolean) => !isDrawed);
export const getTemplateType = pipe(getDrawed, isAdvanced);

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
