import { Observable } from 'rxjs';

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pluck } from 'rxjs/operators';

@Component({
    selector: 'dot-form-builder',
    templateUrl: './dot-form-builder.component.html',
    styleUrls: ['./dot-form-builder.component.scss']
})
export class DotFormBuilderComponent implements OnInit {
    haveLicense$: Observable<boolean>;

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.haveLicense$ = this.route.data.pipe(pluck('haveLicense'));
    }
}
