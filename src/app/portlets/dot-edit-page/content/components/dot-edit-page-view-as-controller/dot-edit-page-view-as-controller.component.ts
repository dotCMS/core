import { Component, Input, OnInit } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { DotLanguage } from '@models/dot-language/dot-language.model';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { DotMessageService } from '@services/dot-messages-service';
import { Observable } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotPersonalizeService } from '@services/dot-personalize/dot-personalize.service';
import { take, map } from 'rxjs/operators';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotRenderedPageState, DotPageMode } from '@portlets/dot-edit-page/shared/models';

@Component({
    selector: 'dot-edit-page-view-as-controller',
    templateUrl: './dot-edit-page-view-as-controller.component.html',
    styleUrls: ['./dot-edit-page-view-as-controller.component.scss']
})
export class DotEditPageViewAsControllerComponent implements OnInit {
    @Input() pageState: DotRenderedPageState;

    isEnterpriseLicense$: Observable<boolean>;
    messages: { [key: string]: string } = {};
    previewDeviceLabel$: Observable<string>;

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotLicenseService: DotLicenseService,
        private dotPageStateService: DotPageStateService,
        private dotPersonalizeService: DotPersonalizeService
    ) {}

    ngOnInit(): void {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.previewDeviceLabel$ = this.dotMessageService
            .getMessages(['editpage.viewas.previewing'])
            .pipe(
                take(1),
                map((messages: { [key: string]: string }) => messages['editpage.viewas.previewing'])
            );
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
     * @param {DotPersona} { keyTag }
     * @memberof DotEditPageViewAsControllerComponent
     */
    deletePersonalization({ keyTag }: DotPersona): void {
        this.dotAlertConfirmService.confirm({
            header: 'Delete personalization',
            message: `Are you sure you want delete personalization for <b>${
                this.pageState.viewAs.persona.name
            }</b>? This action cannot be undone.`,
            accept: () => {
                this.dotPersonalizeService
                    .despersonalized(this.pageState.page.identifier, keyTag)
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
