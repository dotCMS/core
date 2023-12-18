import { BehaviorSubject, Observable, forkJoin } from 'rxjs';

import { AsyncPipe, JsonPipe, Location, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

import { map, tap, switchMap } from 'rxjs/operators';

import {
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentAsideComponent } from '../../components/dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentToolbarComponent } from '../../components/dot-edit-content-toolbar/dot-edit-content-toolbar.component';
import { EditContentPayload } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';

@Component({
    selector: 'dot-edit-content-form-layout',
    standalone: true,
    imports: [
        NgIf,
        JsonPipe,
        AsyncPipe,
        DotMessagePipe,
        ButtonModule,
        ToastModule,
        DotEditContentFormComponent,
        DotEditContentAsideComponent,
        DotEditContentToolbarComponent
    ],
    templateUrl: './edit-content.layout.component.html',
    styleUrls: ['./edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotEditContentService,
        DotMessageService,
        MessageService
    ]
})
export class EditContentLayoutComponent implements OnInit {
    private activatedRoute = inject(ActivatedRoute);

    public contentType = this.activatedRoute.snapshot.params['contentType'];
    public identifier = this.activatedRoute.snapshot.params['id'];

    private readonly dotEditContentService = inject(DotEditContentService);
    private readonly workflowActionService = inject(DotWorkflowsActionsService);
    private readonly WorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    private readonly messageService = inject(MessageService);
    private readonly dotMessageService = inject(DotMessageService);

    private readonly location = inject(Location);

    formValue: Record<string, string>;
    isContentSaved = false;

    data$: BehaviorSubject<EditContentPayload> = new BehaviorSubject<EditContentPayload>(null);

    ngOnInit() {
        if (this.contentType) {
            this.fetchNewContent();
        } else if (this.identifier) {
            this.fetchContent();
        }
    }

    /**
     * Set the form value to be saved.
     *
     * @param {Record<string, string>} formValue - An object containing the key-value pairs of the contentlet to be saved.
     * @memberof EditContentLayoutComponent
     */
    setFormValue(formValue: Record<string, string>) {
        this.formValue = formValue;
    }

    private fetchNewContent() {
        forkJoin([
            this.dotEditContentService.getContentTypeFormData(this.contentType),
            this.workflowActionService.getDefaultActions(this.contentType)
        ])
            .pipe(
                map(([{ layout, fields }, actions]) => {
                    return {
                        layout,
                        fields,
                        contentType: this.contentType,
                        actions
                    };
                })
            )
            .subscribe((data) => {
                this.data$.next(data);
            });
    }

    private fetchContent() {
        forkJoin([
            this.getContentletaAndForm(),
            this.workflowActionService.getByInode(this.identifier, DotRenderMode.EDITING)
        ])
            .pipe(
                map(([data, actions]) => {
                    return {
                        ...data,
                        actions
                    };
                })
            )
            .subscribe((data) => {
                this.data$.next(data);
            });
    }

    private getContentletaAndForm(): Observable<EditContentPayload> {
        return this.dotEditContentService.getContentById(this.identifier).pipe(
            switchMap((contentlet) => {
                const contentType = contentlet.contentType;

                return this.dotEditContentService
                    .getContentTypeFormData(contentType)
                    .pipe(
                        map(({ layout, fields }) => ({ layout, fields, contentlet, contentType }))
                    );
            })
        );
    }

    private updateURL(inode: string) {
        this.location.replaceState(`/content/${inode}`); // Replace the URL with the new inode without reloading the page
    }

    private showSuccessMessage(detail: string) {
        this.messageService.add({
            severity: 'success',
            summary: this.dotMessageService.get('dot.common.message.success'),
            detail
        });
    }

    handleAction(action: DotCMSWorkflowAction): void {
        const payload = {
            contentlet: {
                ...this.formValue
            }
        };

        this.WorkflowActionsFireService.fireTo(this.identifier, action.id, payload)
            .pipe(
                tap(({ inode }) => {
                    this.identifier = inode;
                    this.updateURL(inode);
                }),
                switchMap(() =>
                    this.workflowActionService.getByInode(this.identifier, DotRenderMode.EDITING)
                )
            )
            .subscribe(
                (actions) => {
                    this.data$.next({
                        ...this.data$.value,
                        actions
                    });
                    this.showSuccessMessage(action.name);
                },
                (error) => {
                    this.messageService.add({
                        severity: 'error',
                        summary: this.dotMessageService.get('dot.common.message.error'),
                        detail: error.message
                    });
                }
            );
    }
}
