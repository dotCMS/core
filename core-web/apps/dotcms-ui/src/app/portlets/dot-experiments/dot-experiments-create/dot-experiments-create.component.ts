import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ActivatedRoute } from '@angular/router';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { Observable } from 'rxjs';
import {
    DotExperimentCreateStore,
    DotExperimentsCreateStore
} from '@portlets/dot-experiments/dot-experiments-create/store/dot-experiments-create-store.service';
import { DotExperiment } from '@portlets/dot-experiments/shared/models/dot-experiments.model';

interface CreateForm {
    pageId: FormControl<string>;
    name: FormControl<string>;
    description: FormControl<string>;
}

@Component({
    selector: 'dot-experiments-create',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,

        DotSidebarDirective,
        DotSidebarHeaderComponent,
        DotMessagePipeModule,
        DotFieldValidationMessageModule,
        UiDotIconButtonModule,

        // PrimeNg
        InputTextareaModule,
        InputTextModule,
        SidebarModule,
        ButtonModule
    ],
    templateUrl: './dot-experiments-create.component.html',
    styleUrls: ['./dot-experiments-create.component.scss'],
    providers: [DotExperimentsCreateStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsCreateComponent implements OnInit {
    readonly pageId = this.route.parent?.parent?.parent.snapshot.params.pageId;
    vm$: Observable<DotExperimentCreateStore> = this.dotExperimentsCreateStore.state$;

    form: FormGroup<CreateForm>;

    constructor(
        private readonly dotExperimentsCreateStore: DotExperimentsCreateStore,
        private readonly fb: FormBuilder,
        private readonly route: ActivatedRoute
    ) {}

    ngOnInit(): void {
        this.initForm();
    }

    handleSubmit() {
        const formValues = this.form.value as Pick<
            DotExperiment,
            'pageId' | 'name' | 'description'
        >;
        this.dotExperimentsCreateStore.addExperiments(formValues);
    }

    closeSidebar() {
        this.dotExperimentsCreateStore.setCloseSidebar();
    }

    private initForm() {
        this.form = new FormGroup<CreateForm>({
            pageId: new FormControl<string>(this.pageId, {
                nonNullable: true,
                validators: [Validators.required]
            }),
            name: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(255)]
            }),
            description: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(255)]
            })
        });
    }
}
