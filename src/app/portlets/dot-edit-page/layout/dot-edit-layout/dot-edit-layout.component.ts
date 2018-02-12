import { Component, OnInit } from '@angular/core';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { ActivatedRoute } from '@angular/router';
import { getTemplateType } from '../../main/dot-edit-page-main/dot-edit-page-main.component';
import { Observable } from 'rxjs/Observable';

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
        this.isAdvancedTemplate = this.route.parent.parent.data.let(getTemplateType);
        this.pageView = this.route.parent.parent.data.pluck('pageView');
    }
}
