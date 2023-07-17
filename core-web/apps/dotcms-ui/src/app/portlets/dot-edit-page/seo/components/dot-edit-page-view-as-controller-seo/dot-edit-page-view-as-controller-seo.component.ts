import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotDeviceSelectorModule } from '@components/dot-device-selector/dot-device-selector.module';
import { DotLanguageSelectorModule } from '@components/dot-language-selector/dot-language-selector.module';
import { DotPersonaSelectorModule } from '@components/dot-persona-selector/dot-persona.selector.module';
import {
    DotAlertConfirmService,
    DotLicenseService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import {
    DotDevice,
    DotLanguage,
    DotPageMode,
    DotPageRenderState,
    DotPersona,
    DotVariantData
} from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';

@Component({
    selector: 'dot-edit-page-view-as-controller-seo',
    templateUrl: './dot-edit-page-view-as-controller-seo.component.html',
    styleUrls: ['./dot-edit-page-view-as-controller-seo.component.scss'],
    standalone: true,
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorModule,
        DotDeviceSelectorModule,
        DotPipesModule,
        DotIconModule,
        CheckboxModule
    ]
})
export class DotEditPageViewAsControllerSeoComponent implements OnInit {
    isEnterpriseLicense$: Observable<boolean>;
    @Input() pageState: DotPageRenderState;
    @Input() variant: DotVariantData | null = null;

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
