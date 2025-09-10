import { Observable } from 'rxjs';

import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pluck } from 'rxjs/operators';

@Component({
    selector: 'dot-form-builder',
    templateUrl: './dot-form-builder.component.html',
    styleUrls: ['./dot-form-builder.component.scss'],
    standalone: false
})
export class DotFormBuilderComponent implements OnInit {
    private route = inject(ActivatedRoute);

    haveLicense$: Observable<boolean>;

    ngOnInit() {
        this.haveLicense$ = this.route.data.pipe(pluck('haveLicense'));
    }
}
