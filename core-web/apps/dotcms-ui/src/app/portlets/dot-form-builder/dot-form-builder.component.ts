import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotNotLicenseComponent } from '@dotcms/ui';

import { DotContentTypesPortletComponent } from '../shared/dot-content-types-listing/dot-content-types.component';

@Component({
    selector: 'dot-form-builder',
    templateUrl: './dot-form-builder.component.html',
    styleUrls: ['./dot-form-builder.component.scss'],
    imports: [AsyncPipe, DotContentTypesPortletComponent, DotNotLicenseComponent]
})
export class DotFormBuilderComponent implements OnInit {
    private route = inject(ActivatedRoute);

    haveLicense$: Observable<boolean>;

    ngOnInit() {
        this.haveLicense$ = this.route.data.pipe(map((x: any) => x?.haveLicense));
    }
}
