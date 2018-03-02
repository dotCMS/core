import { Component, OnInit } from '@angular/core';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { getTemplateTypeFlag } from '../../../../api/util/lib';

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent implements OnInit {
    isAdvancedTemplate: Observable<boolean>;
    pageView: Observable<DotPageView>;

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.pageView = this.route.data.pluck('content');
        this.isAdvancedTemplate = this.pageView.let(getTemplateTypeFlag);
    }
}
