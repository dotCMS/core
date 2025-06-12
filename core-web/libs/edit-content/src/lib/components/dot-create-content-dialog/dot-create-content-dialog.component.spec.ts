import { of, throwError, Subject } from 'rxjs';

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotContentTypeService } from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentlet,
    ComponentStatus,
    FeaturedFlags
} from '@dotcms/dotcms-models';

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
    let contentTypeService: Partial<DotContentTypeService>;
    let dialogRef: Partial<DynamicDialogRef>;
    let onCloseSubject: Subject<DotCMSContentlet | null>;

    // Mock required services for DotEditContentLayoutComponent
    const mockCurrentUserService = {};
    const mockHttpErrorManagerService = {};

    const mockContentType: DotCMSContentType = {
        id: 'blog-post',
        name: 'Blog Post',
        variable: 'blogPost',
        baseType: 'CONTENT',
        clazz: '',
        defaultType: false,
        fields: [],
        fixed: false,
        folder: '',
        host: '',
        iDate: 0,
        layout: [],
        modDate: 0,
        multilingualable: false,
        nEntries: 0,
        system: false,
        versionable: false,
        workflows: [],
        metadata: {
            [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
        }
    };

    beforeEach(async () => {
        onCloseSubject = new Subject();
        dialogConfig = { data: null } as DynamicDialogConfig;
        contentTypeService = { getContentType: jest.fn() } as Partial<DotContentTypeService>;
        (contentTypeService.getContentType as jest.Mock).mockReturnValue = jest.fn();
        dialogRef = {
            onClose: onCloseSubject.asObservable(),
            close: jest.fn()
        } as Partial<DynamicDialogRef>;

        await TestBed.configureTestingModule({
            imports: [DotEditContentDialogComponent],
            providers: [
                { provide: DotContentTypeService, useValue: contentTypeService },
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

    it('should throw if no dialog data is provided', () => {
        dialogConfig.data = null;
        expect(() => fixture.detectChanges()).toThrow(
            'Dialog data is required for edit content dialog'
        );
    });

    it('should throw if new mode and no contentTypeId', () => {
        dialogConfig.data = { mode: 'new' };
        expect(() => fixture.detectChanges()).toThrow(
            'Content type ID is required when creating new content'
        );
    });

    it('should throw if edit mode and no contentletInode', () => {
        dialogConfig.data = { mode: 'edit' };
        expect(() => fixture.detectChanges()).toThrow(
            'Contentlet inode is required when editing existing content'
        );
    });

    it('should show loading then loaded state for new content', async () => {
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };
        (contentTypeService.getContentType as jest.Mock).mockReturnValue(of(mockContentType));
        fixture.detectChanges();
        expect(component['state']()).toBe(ComponentStatus.LOADING);
        await fixture.whenStable();
        expect(component['state']()).toBe(ComponentStatus.LOADED);
    });

    it('should show error state if content type fails to load', async () => {
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };
        (contentTypeService.getContentType as jest.Mock).mockReturnValue(
            throwError(() => new Error('fail'))
        );
        fixture.detectChanges();
        await fixture.whenStable();
        expect(component['state']()).toBe(ComponentStatus.ERROR);
        expect(component['error']()).toContain('fail');
    });

    it('should set loaded state for edit mode', () => {
        dialogConfig.data = { mode: 'edit', contentletInode: 'inode' };
        fixture.detectChanges();
        expect(component['state']()).toBe(ComponentStatus.LOADED);
    });

    it('should show legacy placeholder if not compatible', async () => {
        dialogConfig.data = { mode: 'new', contentTypeId: 'blog-post' };
        (contentTypeService.getContentType as jest.Mock).mockReturnValue(
            of({
                ...mockContentType,
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
            })
        );
        fixture.detectChanges();
        await fixture.whenStable();
        expect(component['isCompatible']()).toBe(false);
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
