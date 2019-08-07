import { Component, OnInit, Input, EventEmitter, Output, OnChanges } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotRenderedPageState, DotPageMode } from '@portlets/dot-edit-page/shared/models';


@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges {
    @Input()
    pageState: DotRenderedPageState;

    @Output()
    whatschange = new EventEmitter<boolean>();

    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;
    whatschangeLabel$: Observable<string>;

    constructor(
        public dotMessageService: DotMessageService,
        private dotLicenseService: DotLicenseService
    ) {}

    ngOnInit() {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.whatschangeLabel$ = this.dotMessageService
            .getMessages(['dot.common.whats.changed'])
            .pipe(
                map((messages: { [key: string]: string }) => messages['dot.common.whats.changed'])
            );
    }

    ngOnChanges(): void {
        this.showWhatsChanged = this.pageState.state.mode === DotPageMode.PREVIEW;
    }
}
