import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { pluck } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { DotUnlicensedPortlet } from './resolvers/dot-form-resolver.service';

@Component({
    selector: 'dot-form-builder',
    templateUrl: './dot-form-builder.component.html',
    styleUrls: ['./dot-form-builder.component.scss']
})
export class DotFormBuilderComponent implements OnInit {
    unlicensed$: Observable<DotUnlicensedPortlet>;

    constructor(private route: ActivatedRoute) {}

    ngOnInit() {
        this.unlicensed$ = this.route.data.pipe(pluck('unlicensed'));
    }
}
