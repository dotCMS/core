import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TemplateContainersCacheService } from '@portlets/dot-edit-page/template-containers-cache.service';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotPortletToolbarActions } from '@shared/models/dot-portlet-toolbar.model/dot-portlet-toolbar-actions.model';
import { DialogService } from 'primeng/dynamicdialog';
import { Observable, zip } from 'rxjs';
import { map, mergeAll, pluck, startWith, take } from 'rxjs/operators';
import { DotTemplatePropsComponent } from './dot-template-props/dot-template-props.component';

import * as _ from 'lodash';

@Component({
    selector: 'dot-dot-template-designer',
    templateUrl: './dot-template-designer.component.html',
    styleUrls: ['./dot-template-designer.component.scss']
})
export class DotTemplateDesignerComponent implements OnInit {
    form: FormGroup;
    title: string;

    private originalData: any;

    portletActions$: Observable<DotPortletToolbarActions>;

    constructor(
        private activatedRoute: ActivatedRoute,
        private fb: FormBuilder,
        private templateContainersCacheService: TemplateContainersCacheService,
        private dotTemplateService: DotTemplatesService,
        private dialogService: DialogService
    ) {}

    ngOnInit(): void {
        this.activatedRoute.data
            .pipe(pluck('template', 'title'), take(1))
            .subscribe((title: string) => {
                this.title = title;
            });

        const inode$ = this.activatedRoute.params.pipe(pluck('inode'));
        const data$ = this.activatedRoute.data.pipe(pluck('template'));

        this.portletActions$ = zip(inode$, data$).pipe(
            take(1),
            map(([inode, template]: [string, any]) => {
                this.templateContainersCacheService.set(template.containers);
                this.form = this.getForm(inode, template);
                this.originalData = { ...this.form.value };

                return this.form.valueChanges.pipe(
                    map((value: any) => {
                        console.log('valueChanges');
                        return this.getToolbarActions(_.isEqual(value, this.originalData));
                    }),
                    startWith(this.getToolbarActions())
                );
            }),
            mergeAll()
        );
    }

    /**
     * Start template properties edition
     *
     * @memberof DotTemplateDesignerComponent
     */
    editTemplateProps(): void {
        this.dialogService.open(DotTemplatePropsComponent, {
            header: 'Template Properties',
            width: '30rem',
            data: {
                template: this.form.value,
                doSomething: (value) => {
                    this.form.setValue(value);
                    this.originalData = { ...this.form.value };
                    this.title = value.title;
                }
            }
        });
    }

    private getForm(inode: string, template: any): FormGroup {
        const { title, friendlyName } = template;

        return this.fb.group({
            inode,
            title,
            friendlyName,
            layout: this.fb.group({
                body: template.layout.body,
                header: template.layout.header,
                footer: template.layout.footer,
                sidebar: template.layout.sidebar
            })
        });
    }

    private getToolbarActions(disabled = true): DotPortletToolbarActions {
        return {
            primary: [
                {
                    label: 'Save',
                    disabled: disabled,
                    command: () => {
                        this.saveTemplate();
                    }
                }
            ],
            cancel: () => {
                console.log('cancel');
            }
        };
    }

    private saveTemplate(): void {
        this.dotTemplateService
            .update(this.form.value)
            .pipe(take(1))
            .subscribe(
                (res) => {
                    console.log(res);
                },
                (err) => {
                    console.log(err);
                }
            );
    }
}
