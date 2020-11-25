import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { DialogService } from 'primeng/dynamicdialog';

import { DotTemplate } from '@shared/models/dot-edit-layout-designer/dot-template.model';
import { DotPortletToolbarActions } from '@shared/models/dot-portlet-toolbar.model/dot-portlet-toolbar-actions.model';

import { DotTemplatePropsComponent } from './dot-template-props/dot-template-props.component';
import { DotTemplateItem, DotTemplateStore } from './store/dot-template.store';

@Component({
    selector: 'dot-template-create-edit',
    templateUrl: './dot-template-create-edit.component.html',
    styleUrls: ['./dot-template-create-edit.component.scss'],
    providers: [DotTemplateStore]
})
export class DotTemplateCreateEditComponent implements OnInit, OnDestroy {
    actions$ = this.store.didTemplateChanged$.pipe(
        map((didChange: boolean) => this.getActions(didChange))
    );
    apiLink$ = this.store.apiLink$;
    template$ = this.store.template$;

    form: FormGroup;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotTemplateStore,
        private fb: FormBuilder,
        private dialogService: DialogService
    ) {}

    ngOnInit() {
        this.template$.pipe(takeUntil(this.destroy$)).subscribe((template: DotTemplateItem) => {
            if (this.form) {
                const { type, ...value } = template;
                this.form.setValue(value);
            } else {
                this.form = this.getForm(template);
            }

            if (!template.identifier) {
                this.createTemplate();
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Start template properties edition
     *
     * @memberof DotTemplateCreateEditComponent
     */
    editTemplateProps(): void {
        this.dialogService.open(DotTemplatePropsComponent, {
            header: 'Template Properties',
            width: '30rem',
            data: {
                template: this.form.value,
                onSave: (value: DotTemplateItem) => {
                    this.store.saveTemplate(value);
                }
            }
        });
    }

    /**
     * Save template to store
     *
     * @memberof DotTemplateCreateEditComponent
     */
    saveTemplate({ layout }: DotTemplate): void {
        this.store.saveTemplate({
            ...this.form.value,
            layout
        });
    }

    private createTemplate(): void {
        this.dialogService.open(DotTemplatePropsComponent, {
            header: 'Create new template',
            width: '30rem',
            closable: false,
            closeOnEscape: false,
            data: {
                template: this.form.value,
                onSave: (value: DotTemplateItem) => {
                    this.store.createTemplate(value);
                },
                onCancel: () => {
                    this.store.cancelCreate();
                }
            }
        });
    }

    private getForm(template: DotTemplateItem): FormGroup {
        if (template.type === 'design') {
            return this.fb.group({
                title: [template.title, Validators.required],
                layout: this.fb.group(template.layout),
                identifier: template.identifier,
                friendlyName: template.friendlyName,
                theme: template.theme
            });
        }

        return this.fb.group({
            title: [template.title, Validators.required],
            body: template.body,
            identifier: template.identifier,
            friendlyName: template.friendlyName,
            drawed: template.drawed
        });
    }

    private getActions(disabled = true): DotPortletToolbarActions {
        return {
            primary: [
                {
                    label: 'Save',
                    disabled: disabled,
                    command: () => {
                        this.store.saveTemplate(this.form.value);
                    }
                }
            ],
            cancel: () => {
                console.log('cancel');
            }
        };
    }
}
