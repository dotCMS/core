import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotDevice } from '@dotcms/dotcms-models';
import { DotMessageService } from '@dotcms/data-access';
import { DotLicenseService } from '@dotcms/data-access';
import { DotPersonalizeService } from '@dotcms/data-access';
import { DotAlertConfirmService } from '@dotcms/data-access';
import { DotPageRenderState } from '@dotcms/dotcms-models';
import { DotPageMode } from '@dotcms/dotcms-models';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';

@Component({
    selector: 'dot-edit-page-view-as-controller',
    templateUrl: './dot-edit-page-view-as-controller.component.html',
    styleUrls: ['./dot-edit-page-view-as-controller.component.scss']
})
export class DotEditPageViewAsControllerComponent implements OnInit {
    isEnterpriseLicense$: Observable<boolean>;
    @Input() pageState: DotPageRenderState;

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotLicenseService: DotLicenseService,
        public dotPageStateService: DotPageStateService,
        private dotPersonalizeService: DotPersonalizeService
    ) {}

    ngOnInit(): void {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
    }

    /**
     * Handle the changes in Persona Selector.
     *
     * @param DotPersona persona
     * @memberof DotEditPageViewAsControllerComponent
     */
    changePersonaHandler(persona: DotPersona): void {
        this.dotPageStateService.setPersona(persona);
    }

    /**
     * Handle changes in Language Selector.
     *
     * @param DotLanguage language
     * @memberof DotEditPageViewAsControllerComponent
     */
    changeLanguageHandler({ id }: DotLanguage): void {
        this.dotPageStateService.setLanguage(id);
    }

    /**
     * Handle changes in Device Selector.
     *
     * @param DotDevice device
     * @memberof DotEditPageViewAsControllerComponent
     */
    changeDeviceHandler(device: DotDevice): void {
        this.dotPageStateService.setDevice(device);
    }

    /**
     * Remove personalization for the current page and set the new state to the page
     *
     * @param {DotPersona} persona
     * @memberof DotEditPageViewAsControllerComponent
     */
    deletePersonalization(persona: DotPersona): void {
        this.dotAlertConfirmService.confirm({
            header: this.dotMessageService.get('editpage.personalization.delete.confirm.header'),
            message: this.dotMessageService.get(
                'editpage.personalization.delete.confirm.message',
                persona.name
            ),
            accept: () => {
                this.dotPersonalizeService
                    .despersonalized(this.pageState.page.identifier, persona.keyTag)
                    .pipe(take(1))
                    .subscribe(() => {
                        this.dotPageStateService.setLock(
                            {
                                mode: DotPageMode.PREVIEW
                            },
                            false
                        );
                    });
            }
        });
    }
}
