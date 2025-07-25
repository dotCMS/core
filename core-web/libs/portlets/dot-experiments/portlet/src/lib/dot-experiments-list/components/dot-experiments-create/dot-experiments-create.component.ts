import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SidebarModule } from 'primeng/sidebar';

import { DotExperiment, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSidebarDirective,
    DotSidebarHeaderComponent,
    DotTrimInputDirective,
    DotValidators
} from '@dotcms/ui';

import {
    DotExperimentsListStore,
    VmCreateExperiments
} from '../../store/dot-experiments-list-store';

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
        DotMessagePipe,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        // PrimeNg
        InputTextareaModule,
        InputTextModule,
        SidebarModule,
        ButtonModule,
        DotFieldRequiredDirective,
        DotTrimInputDirective
    ],
    templateUrl: './dot-experiments-create.component.html',
    styleUrls: ['./dot-experiments-create.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsCreateComponent implements OnInit {
    private readonly dotExperimentsListStore = inject(DotExperimentsListStore);

    vm$: Observable<VmCreateExperiments> = this.dotExperimentsListStore.createVm$;

    form: FormGroup<CreateForm>;
    protected readonly maxNameLength = MAX_INPUT_TITLE_LENGTH;

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
                validators: [
                    Validators.required,
                    Validators.maxLength(this.maxNameLength),
                    DotValidators.noWhitespace
                ]
            }),
            description: new FormControl<string>('', {
                validators: [Validators.maxLength(255), DotValidators.noWhitespace]
            })
        });
    }
}
