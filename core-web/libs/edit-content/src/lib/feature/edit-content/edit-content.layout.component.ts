import { Observable, forkJoin, of } from 'rxjs';

import { animate, state, style, transition, trigger } from '@angular/animations';
import { AsyncPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    inject,
    HostBinding,
    signal
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import { switchMap } from 'rxjs/operators';

import {
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore, SIDEBAR_LOCAL_STORAGE_KEY } from './store/edit-content.store';

import { DotEditContentAsideComponent } from '../../components/dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentToolbarComponent } from '../../components/dot-edit-content-toolbar/dot-edit-content-toolbar.component';
import { EditContentPayload } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';

@Component({
    selector: 'dot-edit-content-form-layout',
    standalone: true,
    imports: [
        AsyncPipe,
        DotMessagePipe,
        ButtonModule,
        ToastModule,
        MessagesModule,
        RouterLink,
        DotEditContentFormComponent,
        DotEditContentAsideComponent,
        DotEditContentToolbarComponent,
        ConfirmDialogModule
    ],
    templateUrl: './edit-content.layout.component.html',
    styleUrls: ['./edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotEditContentService,
        MessageService,
        DotEditContentStore
    ],
    animations: [
        trigger('sidebarAnimation', [
            state(
                'false',
                style({
                    'grid-template-columns': '1fr 0rem'
                })
            ),
            state(
                'true',
                style({
                    'grid-template-columns': '1fr 21.875rem'
                })
            ),
            transition('false <=> true', animate('300ms ease-in-out'))
        ])
    ]
})
export class EditContentLayoutComponent implements OnInit {
    @HostBinding('@sidebarAnimation') showSidebar: boolean;

    readonly #store = inject(DotEditContentStore);

    readonly #dotEditContentService = inject(DotEditContentService);
    readonly #workflowActionService = inject(DotWorkflowsActionsService);
    readonly #activatedRoute = inject(ActivatedRoute);

    #$contentType = signal<string>(
        this.#activatedRoute.snapshot.params['contentType']
    ).asReadonly();
    #$initialInode = signal<string>(this.#activatedRoute.snapshot.params['id']).asReadonly();

    #formValue: Record<string, string>;

    vm$: Observable<EditContentPayload> = this.#store.vm$;
    $featuredFlagContentKEY = signal<string>(
        FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
    ).asReadonly();

    ngOnInit(): void {
        const obs$ = !this.#$initialInode()
            ? this.getNewContent(this.#$contentType())
            : this.getExistingContent(this.#$initialInode());

        obs$.subscribe(({ contentType, actions, contentlet }) => {
            this.#store.setState({
                contentType,
                actions,
                contentlet,
                loading: false,
                layout: {
                    showSidebar: this.getSidebarSateFromLocalStorage()
                }
            });
        });

        this.#store.layout$.subscribe((layout) => {
            this.showSidebar = layout.showSidebar;
        });
    }

    /**
     * Toggle the show of the sidebar.
     *
     * @memberof EditContentLayoutComponent
     */
    toggleSidebar() {
        this.#store.updateSidebarState(!this.showSidebar);
    }

    /**
     * Set the form value to be saved.
     *
     * @param {Record<string, string>} formValue - An object containing the key-value pairs of the contentlet to be saved.
     * @memberof EditContentLayoutComponent
     */
    setFormValue(formValue: Record<string, string>) {
        this.#formValue = formValue;
    }

    /**
     * Fire the workflow action.
     *
     * @param {DotCMSWorkflowAction} action
     * @memberof EditContentLayoutComponent
     */
    fireWorkflowAction({ actionId, inode, contentType }): void {
        this.#store.fireWorkflowActionEffect({
            actionId,
            inode,
            data: {
                contentlet: {
                    ...this.#formValue,
                    contentType
                }
            }
        });
    }

    /**
     * Get the content type, actions and contentlet for the given contentTypeVar
     *
     * @private
     * @param {string} contentTypeVar
     * @return {*}
     * @memberof EditContentLayoutComponent
     */
    private getNewContent(contentTypeVar: string) {
        return forkJoin({
            contentType: this.#dotEditContentService.getContentType(contentTypeVar),
            actions: this.#workflowActionService.getDefaultActions(contentTypeVar),
            contentlet: of(null)
        });
    }

    /**
     * Get the contentlet, content type and actions for the given inode
     *
     * @private
     * @param {*} inode
     * @return {*}
     * @memberof EditContentLayoutComponent
     */
    private getExistingContent(inode) {
        return this.#dotEditContentService.getContentById(inode).pipe(
            switchMap((contentlet) => {
                const { contentType } = contentlet;

                return forkJoin({
                    contentType: this.#dotEditContentService.getContentType(contentType),
                    actions: this.#workflowActionService.getByInode(inode, DotRenderMode.EDITING),
                    contentlet: of(contentlet)
                });
            })
        );
    }

    /**
     * Get the sidebar state from local storage
     *
     * @private
     * @return {*}
     * @memberof EditContentLayoutComponent
     */
    private getSidebarSateFromLocalStorage() {
        const localStorageData = localStorage.getItem(SIDEBAR_LOCAL_STORAGE_KEY);

        return localStorageData ? localStorageData === 'true' : true;
    }
}
