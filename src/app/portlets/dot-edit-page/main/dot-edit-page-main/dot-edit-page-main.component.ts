import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit {
    isAdvancedTemplate: Observable<boolean>;

    constructor(private route: ActivatedRoute, private pageViewService: PageViewService) {}

    ngOnInit() {
        this.isAdvancedTemplate = this.route.queryParams
            .pluck('url')
            .mergeMap((url: string) => this.pageViewService.isTemplateAdvanced(url));
    }
}
