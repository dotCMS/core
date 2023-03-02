import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SidebarModule } from 'primeng/sidebar';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotExperiment } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotSidebarDirective } from '@portlets/shared/directives/dot-sidebar.directive';
import { DotSidebarHeaderComponent } from '@shared/dot-sidebar-header/dot-sidebar-header.component';

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
        DotAutofocusModule,

        //PrimeNg
        SidebarModule,
        ButtonModule,
        InputTextModule
    ],
    templateUrl: './dot-experiments-configuration-variants-add.component.html',
    styleUrls: ['./dot-experiments-configuration-variants-add.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsAddComponent implements OnInit {
    @Input()
    isSaving = false;

    @Input()
    isSidebarOpen: boolean;

    /**
     * Emit when the sidebar is closed
     */
    @Output()
    closedSidebar = new EventEmitter<boolean>();

    /**
     * Emit a valid form values
     */
    @Output()
    formValues = new EventEmitter<Pick<DotExperiment, 'name'>>();

    form: FormGroup;

    saveForm(): void {
        const formValues = this.form.value as Pick<DotExperiment, 'name'>;
        this.formValues.emit(formValues);
        this.closedSidebarEvent();
    }

    ngOnInit(): void {
        this.initForm();
    }

    closedSidebarEvent() {
        this.form.reset();
        this.closedSidebar.emit(true);
    }

    private initForm() {
        this.form = new FormGroup({
            name: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(255)]
            })
        });
    }
}
