import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit {
    pageState: Observable<DotRenderedPageState>;

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.pageState = this.route.data.pluck('content');
    }
}
