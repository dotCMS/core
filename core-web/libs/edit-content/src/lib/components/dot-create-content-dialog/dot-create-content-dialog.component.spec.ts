import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { Subject, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DynamicDialogRef, DynamicDialogConfig, DialogService } from 'primeng/dynamicdialog';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotSiteService,
    DotSystemConfigService,
    DotVersionableService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCMSContentlet, ComponentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentDialogComponent } from './dot-create-content-dialog.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';

describe('DotEditContentDialogComponent', () => {
    let spectator: Spectator<DotEditContentDialogComponent>;
    let component: DotEditContentDialogComponent;
    let onCloseSubject: Subject<DotCMSContentlet | null>;
    let closeSpy: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotEditContentDialogComponent,
        imports: [DotMessagePipe],
        providers: [
            mockProvider(ActivatedRoute, {
                snapshot: { params: {} }
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn(() => of({ id: 'test-user' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotEditContentService, {
                getContentById: jest.fn(() => of({}))
            }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn(() => of({}))
            }),
            mockProvider(DotWorkflowsActionsService, {
                getDefaultActions: jest.fn(() => of([])),
                getByInode: jest.fn(() => of([])),
                getWorkFlowActions: jest.fn(() => of([]))
            }),
            mockProvider(DotWorkflowService, {
                getWorkflowStatus: jest.fn(() => of({}))
            }),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(MessageService),
            ConfirmationService,
            mockProvider(DotMessageService, {
                get: jest.fn(() => 'Test Message'),
                init: jest.fn(() => of({}))
            }),
            mockProvider(DotContentletService, {
                getLanguages: jest.fn(() => of([]))
            }),
            mockProvider(DotLanguagesService, {
                getDefault: jest.fn(() => of({}))
            }),
            mockProvider(DialogService),
            mockProvider(DotVersionableService),
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService),
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        schemas: [NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        onCloseSubject = new Subject<DotCMSContentlet | null>();
        // Capture the original mock before DotEditContentLayoutComponent's
        // #interceptDirtyClose() replaces dialogRef.close with its override.
        closeSpy = jest.fn();

        spectator = createComponent({
            providers: [
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        onClose: onCloseSubject.asObservable(),
                        close: closeSpy
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: null }
                }
            ]
        });

        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should set loaded state for new content', () => {
        // Arrange
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };

        // Act
        spectator.detectChanges();

        // Assert
        expect(component['state']()).toBe(ComponentStatus.LOADED);
    });

    it('should set loaded state for edit mode', () => {
        // Arrange
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };

        // Act
        spectator.detectChanges();

        // Assert
        expect(component['state']()).toBe(ComponentStatus.LOADED);
    });

    it('should call onContentSaved callback only after onClose emits', () => {
        const onContentSaved = jest.fn();
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode', onContentSaved };
        spectator.detectChanges();

        const contentlet = { inode: 'inode' } as DotCMSContentlet;
        // The editor reports the save through the host; the dialog tracks it.
        spectator.inject(OverlayEditContentHost, true).reportSaved(contentlet);

        // Callback must NOT fire before the close actually completes
        component.closeDialog();
        expect(onContentSaved).not.toHaveBeenCalled();

        // Fires only when onClose emits (i.e. close was not cancelled by dirty guard)
        onCloseSubject.next(null);
        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
    });

    it('should call onCancel callback only after onClose emits', () => {
        const onCancel = jest.fn();
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode', onCancel };
        spectator.detectChanges();

        component.closeDialog();
        expect(onCancel).not.toHaveBeenCalled();

        onCloseSubject.next(null);
        expect(onCancel).toHaveBeenCalled();
    });

    it('should close dialog with saved contentlet when closeDialog is called and content was saved', () => {
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };
        spectator.detectChanges();

        const contentlet = { inode: 'inode', title: 'Test Content' } as DotCMSContentlet;

        spectator.inject(OverlayEditContentHost, true).reportSaved(contentlet);
        component.closeDialog();

        // closeSpy is the original close fn captured before #interceptDirtyClose overrides it.
        expect(closeSpy).toHaveBeenCalledWith(contentlet);
    });

    it('should close dialog with null when no content was saved', () => {
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };
        spectator.detectChanges();

        component.closeDialog();

        expect(closeSpy).toHaveBeenCalledWith(null);
    });

    it('should render the footer cancel button', () => {
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };
        spectator.detectChanges();

        expect(spectator.query('[data-testid="edit-content-dialog-footer"]')).toBeTruthy();
        expect(spectator.query('[data-testid="edit-content-dialog-cancel-btn"]')).toBeTruthy();
    });

    it('should call closeDialog when the footer cancel button is clicked', () => {
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };
        spectator.detectChanges();

        const closeDialogSpy = jest.spyOn(component, 'closeDialog');
        const cancelBtn = spectator
            .query('[data-testid="edit-content-dialog-cancel-btn"]')
            ?.querySelector('button');
        spectator.click(cancelBtn!);

        expect(closeDialogSpy).toHaveBeenCalledTimes(1);
    });
});
