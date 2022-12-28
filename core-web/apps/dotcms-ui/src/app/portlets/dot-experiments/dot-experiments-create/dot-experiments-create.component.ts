import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { ButtonModule } from 'primeng/button';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { Observable } from 'rxjs';
import {
    DotExperimentCreateStore,
    DotExperimentsCreateStore
} from '@portlets/dot-experiments/dot-experiments-create/store/dot-experiments-create-store';
import { DotExperiment } from '@dotcms/dotcms-models';
import { take } from 'rxjs/operators';
import { DotExperimentsListStore } from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store.service';

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
    vm$: Observable<DotExperimentCreateStore> = this.dotExperimentsCreateStore.state$;

    form: FormGroup<CreateForm>;

    /**
     * Emited when the sidebar is closed
     */
    @Output()
    closedSidebar = new EventEmitter<void>();
    private pageId: string;

    constructor(
        private readonly dotExperimentsCreateStore: DotExperimentsCreateStore,
        private readonly dotExperimentsListStore: DotExperimentsListStore
    ) {}

    ngOnInit(): void {
        this.initForm();
        this.dotExperimentsCreateStore.setOpenSlider();
        this.dotExperimentsListStore.getPage$.pipe(take(1)).subscribe(({ pageId }) => {
            this.form.controls.pageId.setValue(pageId);
        });
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
        this.closedSidebar.emit();
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
