import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild } from '@angular/core';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { DotEditLayoutGridComponent } from '../dot-edit-layout-grid/dot-edit-layout-grid.component';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { Observable } from 'rxjs/Observable';
import { DotLayoutGridBox } from '../../shared/models/dot-layout-grid-box.model';

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent implements OnInit {
    @ViewChild('editLayoutGrid') editLayoutGrid: DotEditLayoutGridComponent;

    pageView: DotPageView;

    constructor(private pageViewService: PageViewService, private route: ActivatedRoute, public router: Router) {}

    ngOnInit(): void {
        this.route.data.pluck('pageView').subscribe((pageView: DotPageView) => {
            this.pageView = pageView;
        });
    }

    /**
     * Get the LayoutBody and call the service to save the layout
     *
     * @memberof DotEditLayoutComponent
     */
    saveLayout(): void {
        const layoutBody: DotLayoutBody = this.editLayoutGrid.getModel();
        const pageView: DotPageView = Object.assign({}, this.pageView, { layout: { body: layoutBody } });
        this.pageViewService.save(pageView).subscribe();
    }
}
