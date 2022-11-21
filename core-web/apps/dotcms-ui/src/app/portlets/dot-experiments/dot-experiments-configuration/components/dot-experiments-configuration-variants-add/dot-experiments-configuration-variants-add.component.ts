import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotExperiment } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { SidebarModule } from 'primeng/sidebar';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { InputTextModule } from 'primeng/inputtext';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';

interface AddForm {
    name: FormControl<string>;
}

@Component({
    selector: 'dot-experiments-configuration-variants-add',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotSidebarHeaderComponent,
        DotMessagePipeModule,
        DotFieldValidationMessageModule,
        DotSidebarDirective,
        //PrimeNg
        SidebarModule,
        ButtonModule,
        InputTextModule
    ],
    templateUrl: './dot-experiments-configuration-variants-add.component.html',
    styleUrls: ['./dot-experiments-configuration-variants-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsAddComponent implements OnInit, OnDestroy {
    /**
     * Emit when the sidebar is closed
     */
    @Output()
    closedSidebar = new EventEmitter<void>();

    vm$ = this.dotExperimentsConfigurationStore.vmVariants$;
    form: FormGroup<AddForm>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnDestroy(): void {
        this.dotExperimentsConfigurationStore.closeSidebar();
    }

    handleSubmit(): void {
        const formValues = this.form.value as Pick<DotExperiment, 'name'>;
        this.dotExperimentsConfigurationStore.addVariant(formValues);
    }

    ngOnInit(): void {
        this.initForm();
        this.dotExperimentsConfigurationStore.openSidebar();
    }

    private initForm() {
        this.form = new FormGroup<AddForm>({
            name: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(255)]
            })
        });
    }
}
