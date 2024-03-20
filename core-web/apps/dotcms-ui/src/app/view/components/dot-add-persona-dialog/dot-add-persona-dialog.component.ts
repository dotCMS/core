import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotCreatePersonaFormComponent } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.component';
import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotDialogActions, DotPersona } from '@dotcms/dotcms-models';

const PERSONA_CONTENT_TYPE = 'persona';

@Component({
    selector: 'dot-add-persona-dialog',
    templateUrl: './dot-add-persona-dialog.component.html',
    styleUrls: ['./dot-add-persona-dialog.component.scss']
})
export class DotAddPersonaDialogComponent implements OnInit {
    @Input() visible = false;
    @Input() personaName: string;
    @Output() createdPersona: EventEmitter<DotPersona> = new EventEmitter();
    @ViewChild('personaForm') personaForm: DotCreatePersonaFormComponent;

    dialogActions: DotDialogActions;

    constructor(
        private dotMessageService: DotMessageService,
        public dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {}

    ngOnInit() {
        this.setDialogActions();
    }

    /**
     * Handle if the form is valid or not to set the disable state of the accept button
     *
     * @param {FormGroup} form
     *
     * @memberof DotAddPersonaDialogComponent
     */
    handlerFormValidState(valid: boolean): void {
        this.dialogActions.accept.disabled = !valid;
    }

    /**
     * Close the dialog and clear the form
     *
     * @memberof DotAddPersonaDialogComponent
     */
    closeDialog(): void {
        this.visible = false;
        this.personaForm.resetForm();
        this.dialogActions.accept.disabled = true;
    }

    private savePersona(): void {
        if (this.personaForm.form.valid) {
            this.dialogActions.accept.disabled = true;
            this.dotWorkflowActionsFireService
                .publishContentletAndWaitForIndex<DotPersona>(
                    PERSONA_CONTENT_TYPE,
                    this.personaForm.form.getRawValue()
                )
                .pipe(take(1))
                .subscribe(
                    (persona: DotPersona) => {
                        this.createdPersona.emit(persona);
                        this.closeDialog();
                    },
                    (error) => {
                        this.dotHttpErrorManagerService.handle(error).pipe(take(1)).subscribe();
                        this.dialogActions.accept.disabled = !this.personaForm.form.valid;
                    }
                );
        }
    }

    private setDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.savePersona();
                },
                label: this.dotMessageService.get('dot.common.dialog.accept'),
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('dot.common.dialog.reject'),
                action: () => {
                    this.closeDialog();
                }
            }
        };
    }
}
