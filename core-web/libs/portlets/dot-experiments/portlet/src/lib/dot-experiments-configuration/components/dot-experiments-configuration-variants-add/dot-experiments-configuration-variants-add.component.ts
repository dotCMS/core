import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import {
    ComponentStatus,
    MAX_INPUT_TITLE_LENGTH,
    StepStatus,
    TrafficProportion
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-variants-add',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotSidebarHeaderComponent,
        DotMessagePipe,
        DotFieldValidationMessageModule,
        DotSidebarDirective,

        //PrimeNg
        SidebarModule,
        ButtonModule,
        InputTextModule,
        AutoFocusModule,
        DotAutofocusModule
    ],
    templateUrl: './dot-experiments-configuration-variants-add.component.html',
    styleUrls: ['./dot-experiments-configuration-variants-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsAddComponent implements OnInit {
    stepStatus = ComponentStatus;
    form: FormGroup;
    vm$: Observable<{
        experimentId: string;
        trafficProportion: TrafficProportion;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.dotExperimentsConfigurationStore.variantsStepVm$;
    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

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
                validators: [Validators.required, Validators.maxLength(50)]
            })
        });
    }
}
