import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';

import { ComponentStatus, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSidebarDirective,
    DotSidebarHeaderComponent,
    DotTrimInputDirective,
    DotValidators
} from '@dotcms/ui';

import {
    ConfigurationVariantStepViewModel,
    DotExperimentsConfigurationStore
} from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-variants-add',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotSidebarHeaderComponent,
        DotMessagePipe,
        DotFieldValidationMessageComponent,
        DotSidebarDirective,
        //PrimeNg
        SidebarModule,
        ButtonModule,
        InputTextModule,
        AutoFocusModule,
        DotAutofocusDirective,
        DotTrimInputDirective
    ],
    templateUrl: './dot-experiments-configuration-variants-add.component.html',
    styleUrls: ['./dot-experiments-configuration-variants-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsAddComponent implements OnInit {
    private readonly dotExperimentsConfigurationStore = inject(DotExperimentsConfigurationStore);

    stepStatus = ComponentStatus;
    form: FormGroup;
    vm$: Observable<ConfigurationVariantStepViewModel> =
        this.dotExperimentsConfigurationStore.variantsStepVm$;
    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;

    ngOnInit(): void {
        this.initForm();
    }

    /**
     * Save variant
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsAddComponent
     */
    saveVariant(experimentId: string) {
        this.dotExperimentsConfigurationStore.addVariant({
            name: this.form.value.name,
            experimentId
        });
        this.form.reset();
    }

    /**
     * Close sidebar
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsAddComponent
     */
    closeSidebar() {
        this.dotExperimentsConfigurationStore.closeSidebar();
    }

    private initForm() {
        this.form = new FormGroup({
            name: new FormControl<string>('', {
                nonNullable: true,
                validators: [
                    Validators.required,
                    Validators.maxLength(50),
                    DotValidators.noWhitespace
                ]
            })
        });
    }
}
