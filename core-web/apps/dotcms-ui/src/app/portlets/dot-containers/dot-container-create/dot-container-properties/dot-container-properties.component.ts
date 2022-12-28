import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import {
    DotContainerPropertiesStore,
    DotContainerPropertiesState
} from '@portlets/dot-containers/dot-container-create/dot-container-properties/store/dot-container-properties.store';
import { MonacoEditor } from '@models/monaco-editor';
import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { pairwise, startWith, take, takeUntil } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';
import { Subject } from 'rxjs';
import { DotContainerPayload, DotContainerStructure } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-container-properties',
    templateUrl: './dot-container-properties.component.html',
    styleUrls: ['./dot-container-properties.component.scss'],
    providers: [DotContainerPropertiesStore]
})
export class DotContainerPropertiesComponent implements OnInit {
    vm$ = this.store.vm$;
    editor: MonacoEditor;
    form: FormGroup;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotContainerPropertiesStore,
        private dotMessageService: DotMessageService,
        private fb: FormBuilder,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotRouterService: DotRouterService
    ) {
        //
    }

    ngOnInit(): void {
        this.store.containerAndStructure$
            .pipe(take(1))
            .subscribe((state: DotContainerPropertiesState) => {
                const { container, containerStructures } = state;
                this.form = this.fb.group({
                    identifier: new FormControl(container?.identifier ?? ''),
                    title: new FormControl(container?.title ?? '', [Validators.required]),
                    friendlyName: new FormControl(container?.friendlyName ?? ''),
                    maxContentlets: new FormControl(container?.maxContentlets ?? 0, {
                        validators: [Validators.required],
                        updateOn: 'blur'
                    }),
                    code: new FormControl(
                        container?.code ?? '',
                        containerStructures.length === 0 ? [Validators.required] : null
                    ),
                    preLoop: container?.preLoop ?? '',
                    postLoop: container?.postLoop ?? '',
                    containerStructures: this.fb.array(
                        [],
                        containerStructures.length ? [Validators.minLength(1)] : null
                    )
                });

                this.addContainerFormControl(containerStructures);
                if (this.form.value.identifier) {
                    this.store.updateOriginalFormState(this.form.value);
                }
            });

        this.form.valueChanges
            .pipe(takeUntil(this.destroy$), startWith(this.form.value), pairwise())
            .subscribe(([prevValue, currValue]) => {
                this.store.updateFormStatus({
                    invalidForm: !this.form.valid,
                    container: currValue
                });
                if (this.IsShowContentTypes(prevValue, currValue)) {
                    this.showContentTypeAndCode();
                } else if (this.IsShowClearConfirmationModal(prevValue, currValue)) {
                    this.clearContentConfirmationModal(prevValue.maxContentlets || 1);
                }
            });
    }

    /**
     * check prev value and current value for showing the content type
     * @param {DotContainerPayload} prevValue
     * @param {DotContainerPayload} currValue
     * @memberof DotContainerPropertiesComponent
     */
    IsShowContentTypes(prevValue: DotContainerPayload, currValue: DotContainerPayload) {
        return (
            (prevValue.maxContentlets === 0 || prevValue.maxContentlets === null) &&
            currValue.maxContentlets > 0
        );
    }

    /**
     * check prev value and current value for showing confirmation modal
     * @param {DotContainerPayload} prevValue
     * @param {DotContainerPayload} currValue
     * @memberof DotContainerPropertiesComponent
     */
    IsShowClearConfirmationModal(prevValue: DotContainerPayload, currValue: DotContainerPayload) {
        return (
            (prevValue.maxContentlets > 0 || prevValue.maxContentlets === null) &&
            currValue.maxContentlets === 0
        );
    }

    /**
     * Add Container Strcutures into FormControl
     * @param {DotContainerStructure[]} containerStructures
     * @memberof DotContainerPropertiesComponent
     */
    addContainerFormControl(containerStructures: DotContainerStructure[]) {
        if (containerStructures && containerStructures.length > 0) {
            containerStructures.forEach(
                ({ code, structureId, containerId, containerInode, contentTypeVar }) => {
                    (this.form.get('containerStructures') as FormArray).push(
                        this.fb.group({
                            code: new FormControl(code, [Validators.required]),
                            structureId: new FormControl(structureId, [Validators.required]),
                            containerId: new FormControl(containerId),
                            containerInode: new FormControl(containerInode),
                            contentTypeVar: new FormControl(contentTypeVar)
                        })
                    );
                }
            );

            this.showContentTypeAndCode();
        }
    }

    /**
     * This method shows the Pre- and Post-Loop Inputs.
     *
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    showLoopInput(): void {
        this.store.updatePrePostLoopInputVisibility(true);
    }

    /**
     * This method shows the Content type inputs.
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    showContentTypeAndCode(): void {
        const values = this.form.value;
        if (values.maxContentlets > 0) {
            (this.form.get('containerStructures') as FormArray).setValidators([
                Validators.required,
                Validators.minLength(1)
            ]);
            this.form.get('code').clearValidators();
            this.form.get('code').reset('');
            this.store.loadContentTypesAndUpdateVisibility();
        } else {
            this.form.get('code').setValidators(Validators.required);
            this.form.get('containerStructures').clearValidators();
        }

        this.form.updateValueAndValidity();
    }

    /**
     * Updates or Saves the container based on the identifier form value.
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    save(): void {
        const formValues = this.form.value;
        if (formValues.identifier) {
            this.store.editContainer(formValues);
            this.store.updateOriginalFormState(formValues);
            this.form.updateValueAndValidity();
        } else {
            delete formValues.identifier;
            this.store.saveContainer(formValues);
        }
    }

    /**
     * Updates containerStructures based on tab data
     *
     * @param {MenuItem[]} containerStructures
     * @return {void}
     * @memberof DotContainerPropertiesComponent
     */
    updateContainerStructure(containerStructures: MenuItem[]): void {
        const addInContainerStructure = this.form.get('containerStructures') as FormArray;
        // clear containerStructures array
        (this.form.get('containerStructures') as FormArray).clear();
        containerStructures.forEach(({ state }: MenuItem) => {
            addInContainerStructure.push(
                this.fb.group({
                    structureId: new FormControl(state.contentType.variable ?? '', [
                        Validators.required
                    ]),
                    code: new FormControl(state?.code || '', [
                        Validators.required,
                        Validators.minLength(2)
                    ])
                })
            );
        });
        this.form.updateValueAndValidity();
    }

    /**
     * This method navigates the user back to previous page.
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    cancel(): void {
        this.dotRouterService.goToURL('/containers');
    }

    /**
     * Opens modal for confirmation.
     * @param {number} lastValue
     * @memberof DotContainerPropertiesComponent
     */
    clearContentConfirmationModal(lastValue: number): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.clearContentTypesAndCode();
            },
            reject: () => {
                if (this.form.value.maxContentlets === 0 || !this.form.value.maxContentlets) {
                    this.form.get('maxContentlets').setValue(lastValue);
                }
            },
            header: this.dotMessageService.get(
                'message.container.properties.confirm.clear.content.title'
            ),
            message: this.dotMessageService.get(
                'message.container.properties.confirm.clear.content.message'
            )
        });
    }

    /**
     * Clear content types and code and update visibility
     * @private
     * @memberof DotContainerPropertiesComponent
     */
    private clearContentTypesAndCode(): void {
        this.form.get('containerStructures').clearValidators();
        this.form.get('containerStructures').reset();
        this.form.get('preLoop').reset();
        this.form.get('postLoop').reset();
        // clear containerStructures array
        (this.form.get('containerStructures') as FormArray).clear();
        this.form.get('code').addValidators(Validators.required);
        this.form.get('maxContentlets').setValue(0);
        this.form.updateValueAndValidity();

        this.store.updateContentTypeAndPrePostLoopVisibility({
            isContentTypeVisible: false,
            showPrePostLoopInput: false
        });
    }

    /**
     * It returns the form control with the given name
     * @param {string} controlName - The name of the control you want to get.
     * @returns {FormControl} A FormControl
     */
    getFormControl(controlName: string): FormControl {
        return this.form.get(controlName) as FormControl;
    }
}
