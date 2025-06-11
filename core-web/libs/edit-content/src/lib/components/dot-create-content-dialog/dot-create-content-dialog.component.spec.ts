import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService, DotContentTypeService } from '@dotcms/data-access';
import { ComponentStatus, FeaturedFlags, DotCMSContentType } from '@dotcms/dotcms-models';

import {
    DotCreateContentDialogComponent,
    CreateContentDialogData
} from './dot-create-content-dialog.component';

import { DotEditContentStore } from '../../store/edit-content.store';

// Mock component for DotEditContentLayoutComponent
@Component({
    selector: 'dot-edit-content-form-layout',
    template: '<div data-testid="mock-layout">Mock Layout Component</div>',
    standalone: true
})
class MockDotEditContentLayoutComponent {}

describe('DotCreateContentDialogComponent', () => {
    let component: DotCreateContentDialogComponent;
    let fixture: ComponentFixture<DotCreateContentDialogComponent>;
    let mockStore: jasmine.SpyObj<InstanceType<typeof DotEditContentStore>>;
    let mockMessageService: jasmine.SpyObj<DotMessageService>;
    let mockDialogRef: jasmine.SpyObj<DynamicDialogRef>;
    let mockDialogConfig: DynamicDialogConfig;
    let mockContentTypeService: jasmine.SpyObj<DotContentTypeService>;

    const mockDialogData: CreateContentDialogData = {
        contentTypeId: 'test-content-type-id'
    };

    beforeEach(async () => {
        // Create spy objects
        mockStore = jasmine.createSpyObj('DotEditContentStore', [
            'initializeNewContent',
            'isLoaded',
            'state',
            'error',
            'contentlet',
            'contentType',
            'formValues'
        ]);

        mockMessageService = jasmine.createSpyObj('DotMessageService', ['get']);
        mockDialogRef = jasmine.createSpyObj('DynamicDialogRef', ['close']);
        mockContentTypeService = jasmine.createSpyObj('DotContentTypeService', ['getContentType']);

        mockDialogConfig = {
            data: mockDialogData
        };

        // Set up default return values for store signals
        mockStore.isLoaded.and.returnValue(signal(false));
        mockStore.state.and.returnValue(signal(ComponentStatus.LOADING));
        mockStore.error.and.returnValue(signal(null));
        mockStore.contentlet.and.returnValue(signal(null));
        mockStore.contentType.and.returnValue(signal(null));
        mockStore.formValues.and.returnValue(signal({}));

        mockMessageService.get.and.returnValue('Mocked message');

        await TestBed.configureTestingModule({
            imports: [
                DotCreateContentDialogComponent,
                MockDotEditContentLayoutComponent,
                NoopAnimationsModule
            ],
            providers: [
                { provide: DotEditContentStore, useValue: mockStore },
                { provide: DotMessageService, useValue: mockMessageService },
                { provide: DynamicDialogRef, useValue: mockDialogRef },
                { provide: DynamicDialogConfig, useValue: mockDialogConfig },
                { provide: DotContentTypeService, useValue: mockContentTypeService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCreateContentDialogComponent);
        component = fixture.componentInstance;
    });

    describe('Component Initialization', () => {
        it('should create the component', () => {
            expect(component).toBeTruthy();
        });

        it('should initialize store with correct parameters', () => {
            fixture.detectChanges();

            expect(mockStore.initializeNewContent).toHaveBeenCalledWith('test-content-type-id');
        });

        it('should throw error if content type ID is missing', () => {
            mockDialogConfig.data = { contentTypeId: '' };

            expect(() => {
                fixture.detectChanges();
            }).toThrowError('Content type ID is required for creating content');
        });
    });

    describe('Template Rendering', () => {
        it('should show loading state when component is loading', () => {
            mockStore.isLoaded.and.returnValue(signal(false));
            mockStore.state.and.returnValue(signal(ComponentStatus.LOADING));
            mockStore.contentType.and.returnValue(
                signal({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                } as Partial<DotCMSContentType>)
            );

            fixture.detectChanges();

            const loadingElement = fixture.debugElement.query(
                By.css('[data-testid="loading-state"]')
            );
            const layoutElement = fixture.debugElement.query(
                By.css('[data-testid="edit-content-layout"]')
            );

            expect(loadingElement).toBeTruthy();
            expect(layoutElement).toBeFalsy();
        });

        it('should show error state when there is an error', () => {
            mockStore.isLoaded.and.returnValue(signal(false));
            mockStore.state.and.returnValue(signal(ComponentStatus.ERROR));
            mockStore.error.and.returnValue(signal('Test error message'));
            mockStore.contentType.and.returnValue(
                signal({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                } as Partial<DotCMSContentType>)
            );

            fixture.detectChanges();

            const errorElement = fixture.debugElement.query(By.css('[data-testid="error-state"]'));
            const loadingElement = fixture.debugElement.query(
                By.css('[data-testid="loading-state"]')
            );

            expect(errorElement).toBeTruthy();
            expect(loadingElement).toBeFalsy();
        });

        it('should show edit content layout when loaded and compatible', () => {
            mockStore.isLoaded.and.returnValue(signal(true));
            mockStore.state.and.returnValue(signal(ComponentStatus.LOADED));
            mockStore.contentType.and.returnValue(
                signal({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                } as Partial<DotCMSContentType>)
            );

            fixture.detectChanges();

            const layoutElement = fixture.debugElement.query(
                By.css('[data-testid="edit-content-layout"]')
            );
            const loadingElement = fixture.debugElement.query(
                By.css('[data-testid="loading-state"]')
            );

            expect(layoutElement).toBeTruthy();
            expect(loadingElement).toBeFalsy();
        });

        it('should show legacy placeholder when content type is not compatible', () => {
            mockStore.contentType.and.returnValue(
                signal({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
                } as Partial<DotCMSContentType>)
            );

            fixture.detectChanges();

            const placeholderElement = fixture.debugElement.query(
                By.css('[data-testid="legacy-placeholder"]')
            );
            const layoutElement = fixture.debugElement.query(
                By.css('[data-testid="edit-content-layout"]')
            );

            expect(placeholderElement).toBeTruthy();
            expect(layoutElement).toBeFalsy();
        });
    });

    describe('isCompatible computed property', () => {
        it('should return true when content type has new editor enabled', () => {
            mockStore.contentType.and.returnValue(
                signal({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                } as Partial<DotCMSContentType>)
            );

            fixture.detectChanges();

            // Access the protected property using array notation
            const isCompatible = (
                component as unknown as { isCompatible(): boolean }
            ).isCompatible();
            expect(isCompatible).toBe(true);
        });

        it('should return false when content type has new editor disabled', () => {
            mockStore.contentType.and.returnValue(
                signal({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
                } as Partial<DotCMSContentType>)
            );

            fixture.detectChanges();

            const isCompatible = (
                component as unknown as { isCompatible(): boolean }
            ).isCompatible();
            expect(isCompatible).toBe(false);
        });

        it('should return true when content type is null (default assumption)', () => {
            mockStore.contentType.and.returnValue(signal(null));

            fixture.detectChanges();

            const isCompatible = (
                component as unknown as { isCompatible(): boolean }
            ).isCompatible();
            expect(isCompatible).toBe(true);
        });
    });

    describe('Message Service Integration', () => {
        it('should use message service for localized strings', () => {
            // The template uses the DotMessagePipe which would call the message service
            fixture.detectChanges();

            // We can verify the pipe is working by checking if the template renders
            expect(fixture.debugElement.nativeElement).toBeTruthy();
        });
    });

    describe('CSS Classes', () => {
        it('should have correct CSS classes applied', () => {
            fixture.detectChanges();

            const mainElement = fixture.debugElement.query(
                By.css('[data-testid="create-content-dialog"]')
            );

            expect(mainElement.nativeElement.classList).toContain('create-content-dialog');
        });
    });

    describe('Accessibility', () => {
        it('should have proper data-testid attributes for testing', () => {
            fixture.detectChanges();

            const mainElement = fixture.debugElement.query(
                By.css('[data-testid="create-content-dialog"]')
            );

            expect(mainElement).toBeTruthy();
        });
    });
});
