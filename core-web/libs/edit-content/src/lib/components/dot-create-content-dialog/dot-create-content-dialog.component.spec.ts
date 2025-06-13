import { Subject } from 'rxjs';

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet, ComponentStatus } from '@dotcms/dotcms-models';

import { DotEditContentDialogComponent } from './dot-create-content-dialog.component';

// Mock DotMessagePipe
@Pipe({ name: 'dm' })
class MockDotMessagePipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

describe('DotEditContentDialogComponent', () => {
    let fixture: ComponentFixture<DotEditContentDialogComponent>;
    let component: DotEditContentDialogComponent;
    let dialogConfig: DynamicDialogConfig;
    let dialogRef: Partial<DynamicDialogRef>;
    let onCloseSubject: Subject<DotCMSContentlet | null>;

    // Mock required services for DotEditContentLayoutComponent
    const mockCurrentUserService = {};
    const mockHttpErrorManagerService = {};

    beforeEach(async () => {
        onCloseSubject = new Subject();
        dialogConfig = { data: null } as DynamicDialogConfig;
        dialogRef = {
            onClose: onCloseSubject.asObservable(),
            close: jest.fn()
        } as Partial<DynamicDialogRef>;

        await TestBed.configureTestingModule({
            imports: [DotEditContentDialogComponent],
            providers: [
                { provide: DynamicDialogRef, useValue: dialogRef },
                { provide: DynamicDialogConfig, useValue: dialogConfig },
                { provide: 'DotCurrentUserService', useValue: mockCurrentUserService },
                { provide: 'DotHttpErrorManagerService', useValue: mockHttpErrorManagerService },
                { provide: MockDotMessagePipe, useClass: MockDotMessagePipe }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentDialogComponent);
        component = fixture.componentInstance;
    });

    it('should set loaded state for new content', () => {
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };
        fixture.detectChanges();
        expect(component['state']()).toBe(ComponentStatus.LOADED);
    });

    it('should set loaded state for edit mode', () => {
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };
        fixture.detectChanges();
        expect(component['state']()).toBe(ComponentStatus.LOADED);
    });

    it('should call onContentSaved callback when dialog closes and content was saved', () => {
        const onContentSaved = jest.fn();
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode', onContentSaved };
        fixture.detectChanges();
        const contentlet = { inode: 'inode' } as DotCMSContentlet;
        component.onContentSaved(contentlet);
        onCloseSubject.next();
        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
    });

    it('should call onCancel callback when closeDialog is called', () => {
        const onCancel = jest.fn();
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode', onCancel };
        fixture.detectChanges();
        component.closeDialog();
        expect(onCancel).toHaveBeenCalled();
    });
});
