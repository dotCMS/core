import { Component, OnInit, Input, EventEmitter, Output, OnChanges } from '@angular/core';
import { Observable } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotPageRenderState, DotPageMode } from '@portlets/dot-edit-page/shared/models';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges {
    @Input() pageState: DotPageRenderState;

    @Output() cancel = new EventEmitter<boolean>();

    @Output() actionFired = new EventEmitter<boolean>();

    @Output() whatschange = new EventEmitter<boolean>();

    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;

    constructor(private dotLicenseService: DotLicenseService) {}

    ngOnInit() {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
    }

    ngOnChanges(): void {
        this.showWhatsChanged =
            this.pageState.state.mode === DotPageMode.PREVIEW &&
            !('persona' in this.pageState.viewAs);
    }

    /**
     * Hide what's change when state change
     *
     * @memberof DotEditPageToolbarComponent
     */
    stateChange(): void {
        if (this.showWhatsChanged) {
            this.showWhatsChanged = false;
            this.whatschange.emit(this.showWhatsChanged);
        }
    }
}
