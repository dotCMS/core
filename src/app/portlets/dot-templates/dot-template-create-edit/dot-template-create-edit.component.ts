import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { DialogService } from 'primeng/dynamicdialog';

import { DotTemplate } from '@shared/models/dot-edit-layout-designer/dot-template.model';

import { DotTemplatePropsComponent } from './dot-template-props/dot-template-props.component';
import { DotTemplateItem, DotTemplateState, DotTemplateStore } from './store/dot-template.store';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-template-create-edit',
    templateUrl: './dot-template-create-edit.component.html',
    styleUrls: ['./dot-template-create-edit.component.scss'],
    providers: [DotTemplateStore]
})
export class DotTemplateCreateEditComponent implements OnInit, OnDestroy {
    vm$ = this.store.vm$;

    form: FormGroup;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotTemplateStore,
        private fb: FormBuilder,
        private dialogService: DialogService,
        private dotMessageServide: DotMessageService
    ) {}

    ngOnInit() {
        this.vm$.pipe(takeUntil(this.destroy$)).subscribe(({ original }: DotTemplateState) => {
            if (this.form) {
                const { type, ...value } = original;
                this.form.setValue(value);
            } else {
                this.form = this.getForm(original);
            }

            if (!original.identifier) {
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
            header: this.dotMessageServide.get('templates.properties.title'),
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

    /**
     * Handle cancel button from designer
     *
     * @memberof DotTemplateCreateEditComponent
     */
    cancelTemplate() {
        this.store.goToTemplateList();
    }

    private createTemplate(): void {
        this.dialogService.open(DotTemplatePropsComponent, {
            header: this.dotMessageServide.get('templates.create.title'),
            width: '30rem',
            closable: false,
            closeOnEscape: false,
            data: {
                template: this.form.value,
                onSave: (value: DotTemplateItem) => {
                    this.store.createTemplate(value);
                },
                onCancel: () => {
                    this.store.goToTemplateList();
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
            friendlyName: template.friendlyName
        });
    }
}
