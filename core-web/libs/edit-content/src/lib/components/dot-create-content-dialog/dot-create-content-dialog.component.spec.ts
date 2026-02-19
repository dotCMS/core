import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
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

describe('DotEditContentDialogComponent', () => {
    let spectator: Spectator<DotEditContentDialogComponent>;
    let component: DotEditContentDialogComponent;
    let onCloseSubject: Subject<DotCMSContentlet | null>;

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

        spectator = createComponent({
            providers: [
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        onClose: onCloseSubject.asObservable(),
                        close: jest.fn()
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

    it('should call onContentSaved callback when dialog closes and content was saved', () => {
        // Arrange
        const onContentSaved = jest.fn();
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode', onContentSaved };
        spectator.detectChanges();

        const contentlet = { inode: 'inode' } as DotCMSContentlet;

        // Act
        component.onContentSaved(contentlet);
        onCloseSubject.next(null);

        // Assert
        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
    });

    it('should call onCancel callback when closeDialog is called', () => {
        // Arrange
        const onCancel = jest.fn();
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode', onCancel };
        spectator.detectChanges();

        // Act
        component.closeDialog();

        // Assert
        expect(onCancel).toHaveBeenCalled();
    });

    it('should close dialog with saved contentlet when closeDialog is called and content was saved', () => {
        // Arrange
        const dialogRef = spectator.inject(DynamicDialogRef);
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };
        spectator.detectChanges();

        const contentlet = { inode: 'inode', title: 'Test Content' } as DotCMSContentlet;

        // Act
        component.onContentSaved(contentlet);
        component.closeDialog();

        // Assert
        expect(dialogRef.close).toHaveBeenCalledWith(contentlet);
    });

    it('should close dialog with null when no content was saved', () => {
        // Arrange
        const dialogRef = spectator.inject(DynamicDialogRef);
        const dialogConfig = spectator.inject(DynamicDialogConfig);
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };
        spectator.detectChanges();

        // Act
        component.closeDialog();

        // Assert
        expect(dialogRef.close).toHaveBeenCalledWith(null);
    });
});
