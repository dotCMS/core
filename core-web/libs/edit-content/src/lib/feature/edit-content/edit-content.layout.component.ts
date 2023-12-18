import { Observable } from 'rxjs';

import { AsyncPipe, JsonPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

import { map, skip, tap } from 'rxjs/operators';

import {
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore } from './store/edit-content.store';

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
        MessageService,
        DotEditContentStore
    ]
})
export class EditContentLayoutComponent implements OnInit {
    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly store = inject(DotEditContentStore);

    contentType = this.activatedRoute.snapshot.params['contentType'];
    identifier = this.activatedRoute.snapshot.params['id'];
    formValue: Record<string, string>;
    vm$: Observable<EditContentPayload> = this.store.vm$.pipe(
        skip(1),
        tap(({ contentType, contentlet }) => {
            this.contentType = contentlet?.contentType || contentType?.variable;
            this.identifier = contentlet?.identifier;
        }),
        map(({ actions, contentType, contentlet }) => ({
            actions,
            contentType: this.contentType,
            layout: contentType?.layout || [],
            fields: contentType?.fields || [],
            contentlet
        }))
    );

    ngOnInit() {
        this.store.loadContentEffect({
            isNewContent: !this.identifier,
            idOrVar: this.identifier || this.contentType
        });
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

    fireAction(action: DotCMSWorkflowAction): void {
        const data = {
            contentlet: {
                ...this.formValue,
                contentType: this.contentType
            }
        };

        this.store.fireWorkflowActionEffect({
            actionId: action.id,
            inode: this.identifier,
            data
        });
    }
}
