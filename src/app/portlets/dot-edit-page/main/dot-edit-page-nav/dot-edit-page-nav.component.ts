import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'dot-edit-page-nav',
    templateUrl: './dot-edit-page-nav.component.html',
    styleUrls: ['./dot-edit-page-nav.component.scss'],
})
export class DotEditPageNavComponent implements OnInit {
    constructor(public route: ActivatedRoute) {}

    ngOnInit() {

    }
}
