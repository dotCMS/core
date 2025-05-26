import { ChangeDetectionStrategy, Component, inject, input, model, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import {
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotContentletDepths } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { JsonPipe } from '@angular/common';
import { FormValues } from '../../models/dot-edit-content-form.interface';
import { EditContentConfig } from '../../services/dot-edit-content-orchestrator.service';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

/**
 * Component that displays the edit content layout.
 * Can be used in modals (via DynamicDialogConfig) or drawers (via direct inputs)
 */
@Component({
    selector: 'dot-edit-content-form-layout',
    standalone: true,
    imports: [
        DotMessagePipe,
        ButtonModule,
        ToastModule,
        MessagesModule,
        DotEditContentFormComponent,
        DotEditContentSidebarComponent,
        ConfirmDialogModule,
        JsonPipe
    ],
    providers: [
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotEditContentService,
        DotWorkflowService,
        DotEditContentStore,
        DialogService
    ],
    host: {
        '[class.edit-content--with-sidebar]': '$store.isSidebarOpen()'
    },
    templateUrl: './dot-edit-content.layout.component.html',
    styleUrls: ['./dot-edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentLayoutComponent implements OnInit {
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly activatedRoute = inject(ActivatedRoute);

    // Modal support (PrimeNG Dialog)
    readonly dialogConfig = inject(DynamicDialogConfig, { optional: true });
    readonly dialogRef = inject(DynamicDialogRef, { optional: true });

    // Drawer support (Direct inputs)
    readonly inode = input<string>();
    readonly contentType = input<string>();
    readonly mode = input<'edit' | 'new'>('edit');

    readonly $showDialog = model<boolean>(false);

    ngOnInit(): void {
        // Centralized initialization logic
        this.initializeFromDataSource();
    }

    /**
     * Centralized initialization from all possible data sources
     */
    private initializeFromDataSource(): void {
        // 1. Check direct inputs first (drawer/sidebar usage)
        const directInode = this.inode();
        const directContentType = this.contentType();
        const directMode = this.mode();

        if (directInode && directContentType) {
            console.log('🎨 Drawer: Initializing with direct inputs', {
                inode: directInode,
                contentType: directContentType,
                mode: directMode
            });

            if (directMode === 'edit') {
                this.$store.initializeExistingContent({
                    inode: directInode,
                    depth: DotContentletDepths.TWO
                });
            } else if (directMode === 'new') {
                this.$store.initializeNewContent(directContentType);
            }
            return;
        }

        // 2. Check modal configuration (dialog usage)
        if (this.dialogConfig?.data?.config) {
            console.log('📝 Modal: Initializing from dialog config');
            this.handleModalConfig(this.dialogConfig.data.config as EditContentConfig);
            return;
        }

        // 3. Check route parameters (traditional routing)
        const routeParams = this.activatedRoute.snapshot?.params;
        if (routeParams) {
            console.log('🛣️ Route: Initializing from route params', routeParams);
            this.handleRouteParams(routeParams);
            return;
        }

        console.warn('⚠️ Component initialized without any data source');
    }

    /**
     * Handle initialization from route parameters
     */
    private handleRouteParams(params: any): void {
        const contentType = params['contentType'];
        const inode = params['id'];

        if (inode) {
            this.$store.initializeExistingContent({
                inode,
                depth: DotContentletDepths.TWO
            });
        } else if (contentType) {
            this.$store.initializeNewContent(contentType);
        }
    }

    /**
     * Handle modal configuration
     */
    private handleModalConfig(config: EditContentConfig): void {
        console.log('🔧 Handling modal config:', config);

        if (config.mode === 'edit' && config.inode) {
            this.$store.initializeExistingContent({
                inode: config.inode,
                depth: DotContentletDepths.TWO
            });
        } else if (config.mode === 'new' && config.contentType) {
            this.$store.initializeNewContent(config.contentType);
        }
    }

    selectWorkflow() {
        this.$showDialog.set(true);
    }

    onFormChange(value: FormValues) {
        this.$store.onFormChange(value);
    }

    closeMessage(message: 'betaMessage') {
        if (message === 'betaMessage') {
            this.$store.toggleBetaMessage();
        }
    }

    /**
     * Close the container (modal or drawer)
     */
    closeDialog(result?: any): void {
        if (this.dialogRef) {
            // Close modal
            this.dialogRef.close(result);
        } else {
            // For drawer, we'll emit an event or use a service
            // TODO: Implement drawer close mechanism
            console.log('🚪 Drawer close requested with result:', result);
        }
    }

    getDialogData(): any {
        return this.dialogConfig?.data || null;
    }

    isInDialog(): boolean {
        return !!this.dialogRef;
    }

    isInDrawer(): boolean {
        return !this.dialogRef && !!(this.inode() || this.contentType());
    }
}
