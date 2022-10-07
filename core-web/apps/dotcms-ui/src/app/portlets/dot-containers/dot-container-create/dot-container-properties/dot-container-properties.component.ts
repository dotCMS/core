import { Component, OnInit } from '@angular/core';
import {
    FormArray,
    FormControl,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { DotContainerPropertiesStore } from '@portlets/dot-containers/dot-container-create/dot-container-properties/store/dot-container-properties.store';
import { MonacoEditor } from '@models/monaco-editor';
import { DotAlertConfirmService } from '@dotcms/app/api/services/dot-alert-confirm';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActivatedRoute } from '@angular/router';
import { pluck, take } from 'rxjs/operators';
import {
    DotContainerEntity,
    DotContainerStructure
} from '@dotcms/app/shared/models/container/dot-container.model';
import { MenuItem } from 'primeng/api';

@Component({
    selector: 'dot-container-properties',
    templateUrl: './dot-container-properties.component.html',
    styleUrls: ['./dot-container-properties.component.scss'],
    providers: [DotContainerPropertiesStore]
})
export class DotContainerPropertiesComponent implements OnInit {
    vm$ = this.store.vm$;
    editor: MonacoEditor;
    form: UntypedFormGroup;

    containerStructures: DotContainerStructure[];

    private isEdit = false;
    private containerIdentifier: string;

    constructor(
        private store: DotContainerPropertiesStore,
        private dotMessageService: DotMessageService,
        private fb: UntypedFormBuilder,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotRouterService: DotRouterService,
        private activatedRoute: ActivatedRoute
    ) {
        //
    }

    ngOnInit(): void {
        this.activatedRoute.data
            .pipe(pluck('container'), take(1))
            .subscribe((containerEntity: DotContainerEntity) => {
                const { container, containerStructures } = containerEntity;

                this.containerStructures = containerStructures;

                if (container) {
                    this.isEdit = true;
                    this.containerIdentifier = container.identifier;

                    if (container.preLoop || container.postLoop) {
                        this.store.updatePrePostLoopAndContentTypeVisibility({
                            showPrePostLoopInput: true,
                            isContentTypeVisible: true
                        });
                    }
                }

                this.form = this.fb.group({
                    title: new FormControl(container?.title ?? '', [Validators.required]),
                    friendlyName: new FormControl(container?.friendlyName ?? ''),
                    maxContentlets: new FormControl(container?.maxContentlets ?? 0, [
                        Validators.required
                    ]),
                    code: container?.code ?? '',
                    preLoop: container?.preLoop ?? '',
                    postLoop: container?.postLoop ?? '',
                    containerStructures: this.fb.array(containerStructures ?? [])
                });

                this.form.valueChanges.subscribe((values) => {
                    this.store.updateIsContentTypeButtonEnabled(values.maxContentlets > 0);
                });
            });
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
        this.store.updateContentTypeVisibilty(true);
    }

    /**
     * Updates or Saves the container based on the isEdit variable.
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    save(): void {
        const formValues = this.form.value;
        if (this.isEdit) {
            formValues.identifier = this.containerIdentifier;
            this.store.editContainer(formValues);
        } else {
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
                    structureId: state.contentType.variable,
                    code: state?.code || ''
                })
            );
        });
    }

    /**
     * This method navigates the user back to previous page.
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    cancel(): void {
        this.dotRouterService.goToPreviousUrl();
    }

    /**
     * Opens modal on clear content button click.
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    clearContent(): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.store.updateContentTypeVisibilty(false);
                this.form.reset();
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get(
                'message.container.properties.confirm.clear.content.title'
            ),
            message: this.dotMessageService.get(
                'message.container.properties.confirm.clear.content.message'
            )
        });
    }
}
