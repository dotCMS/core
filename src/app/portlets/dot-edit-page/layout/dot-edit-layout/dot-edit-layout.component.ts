import { pluck, filter } from 'rxjs/operators';
import { Component, HostBinding, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { DotPageRenderState } from '../../shared/models/dot-rendered-page-state.model';

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent implements OnInit {
    pageState: Observable<DotPageRenderState>;

    @HostBinding('style.minWidth') width = '100%';

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.pageState = this.route.parent.parent.data.pipe(
            pluck('content'),
            filter((state: DotPageRenderState) => !!state)
        );
    }
}
