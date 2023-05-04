import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { RippleModule } from 'primeng/ripple';
import { SidebarModule } from 'primeng/sidebar';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotExperiment, MAX_INPUT_LENGTH } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import {
    DotExperimentsListStore,
    VmCreateExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

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
        // dotCMS
        DotSidebarDirective,
        DotSidebarHeaderComponent,
        DotMessagePipeModule,
        DotFieldValidationMessageModule,
        UiDotIconButtonModule,
        DotAutofocusModule,
        // PrimeNg
        InputTextareaModule,
        InputTextModule,
        SidebarModule,
        ButtonModule,
        RippleModule
    ],
    templateUrl: './dot-experiments-create.component.html',
    styleUrls: ['./dot-experiments-create.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsCreateComponent implements OnInit {
    vm$: Observable<VmCreateExperiments> = this.dotExperimentsListStore.createVm$;

    form: FormGroup<CreateForm>;
    protected readonly maxNameLength = MAX_INPUT_LENGTH;

    constructor(private readonly dotExperimentsListStore: DotExperimentsListStore) {}

    ngOnInit(): void {
        this.initForm();
    }

    /**
     * Save the experiment
     *
     * @param pageId
     * @memberOf DotExperimentsCreateComponent
     * @return void
     */
    handleSubmit(pageId: string) {
        this.form.get('pageId').setValue(pageId);
        this.dotExperimentsListStore.addExperiments(
            this.form.value as Pick<DotExperiment, 'pageId' | 'name' | 'description'>
        );
    }

    /**
     * Close sidebar
     *
     * @memberOf DotExperimentsCreateComponent
     * @return void
     */
    closeSidebar() {
        this.dotExperimentsListStore.closeSidebar();
    }

    private initForm() {
        this.form = new FormGroup<CreateForm>({
            pageId: new FormControl<string>('', {
                nonNullable: true
            }),
            name: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(this.maxNameLength)]
            }),
            description: new FormControl<string>('', {
                validators: [Validators.maxLength(255)]
            })
        });
    }
}
