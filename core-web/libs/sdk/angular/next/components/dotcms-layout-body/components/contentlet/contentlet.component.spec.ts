import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, ElementRef, Input, Type } from '@angular/core';

import { DotCMSBasicContentlet } from '@dotcms/types';
import { CUSTOM_NO_COMPONENT } from '@dotcms/uve/internal';

import { ContentletComponent } from './contentlet.component';

import { DotCMSStore } from '../../../../store/dotcms.store';
import { FallbackComponent } from '../fallback-component/fallback-component.component';

// Mock component for testing
@Component({
    selector: 'mock-component',
    template: '<div>Mock Component</div>'
})
class MockComponent {
    @Input() contentlet: DotCMSBasicContentlet | undefined;
}

describe('ContentletComponent', () => {
    let spectator: Spectator<ContentletComponent>;
    let component: ContentletComponent;
    let dotcmsStore: jest.Mocked<DotCMSStore>;

    const mockContentlet: DotCMSBasicContentlet = {
        identifier: 'test-contentlet-id',
        inode: 'test-inode',
        contentType: 'test-content-type',
        title: 'Test Contentlet',
        baseType: 'test-basetype'
    } as DotCMSBasicContentlet;

    // Create proper DynamicComponentEntity objects (Promise<Type<any>>)
    const mockComponentsStore = {
        'test-content-type': Promise.resolve(MockComponent as Type<MockComponent>),
        [CUSTOM_NO_COMPONENT]: Promise.resolve(MockComponent as Type<MockComponent>)
    };

    const createComponent = createComponentFactory({
        component: ContentletComponent,
        imports: [FallbackComponent],
        mocks: [DotCMSStore],
        detectChanges: false
    });

    beforeEach(() => {
        dotcmsStore = {
            $isDevMode: jest.fn().mockReturnValue(false),
            store: {
                components: mockComponentsStore
            }
        } as unknown as jest.Mocked<DotCMSStore>;

        spectator = createComponent({
            props: {
                contentlet: mockContentlet,
                containerData: {
                    identifier: 'test-container-id',
                    acceptTypes: 'test-accept-types',
                    maxContentlets: 10,
                    uuid: 'test-uuid'
                }
            },
            providers: [
                {
                    provide: DotCMSStore,
                    useValue: dotcmsStore
                }
            ]
        });

        component = spectator.component;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should set host element attributes correctly', () => {
        spectator.detectChanges();
        const hostElement = spectator.debugElement.nativeElement;

        expect(hostElement.getAttribute('data-dot-object')).toBe('contentlet');
        expect(hostElement.getAttribute('data-dot-identifier')).toBeDefined();
        expect(hostElement.getAttribute('data-dot-basetype')).toBeDefined();
        expect(hostElement.getAttribute('data-dot-title')).toBeDefined();
        expect(hostElement.getAttribute('data-dot-inode')).toBeDefined();
        expect(hostElement.getAttribute('data-dot-container')).toBeDefined();
    });

    it('should set dot attributes in dev mode', () => {
        // Set development mode to true
        dotcmsStore.$isDevMode.mockReturnValue(true);

        spectator.detectChanges();

        // Check if the attributes are set correctly based on the mock contentlet
        expect(component.identifier).not.toBeNull();
        expect(component.basetype).not.toBeNull();
        expect(component.title).not.toBeNull();
        expect(component.inode).not.toBeNull();
        expect(component.type).not.toBeNull();
        expect(component.containerAttribute).not.toBeNull();
    });

    it('should set user component in ngOnChanges', async () => {
        component.ngOnChanges();
        const resolvedComponent = await component.$UserComponent();
        expect(resolvedComponent).toBe(MockComponent);
    });

    it('should set fallback component in ngOnChanges', async () => {
        component.ngOnChanges();
        const resolvedComponent = await component.$UserNoComponent();
        expect(resolvedComponent).toBe(MockComponent);
    });

    it('should display fallback component when in dev mode and UserComponent is not available', () => {
        // Set development mode to true
        spectator.detectChanges();
        dotcmsStore.$isDevMode.mockReturnValue(true);

        // Set UserComponent to null and UserNoComponent to a value
        component.$UserComponent.set(null);
        component.$UserNoComponent.set(Promise.resolve(MockComponent as Type<MockComponent>));
        spectator.detectChanges();

        const fallbackComponent = spectator.query('dotcms-fallback-component');
        expect(fallbackComponent).toBeTruthy();
    });

    it('should display fallback component when in dev mode, UserComponent is not available and UserNoComponent is not available', () => {
        // Set development mode to true
        spectator.detectChanges();
        dotcmsStore.$isDevMode.mockReturnValue(true);

        // Set UserComponent to null and UserNoComponent to null
        component.$UserComponent.set(null);
        component.$UserNoComponent.set(null);
        spectator.detectChanges();

        const fallbackComponent = spectator.query('dotcms-fallback-component');
        expect(fallbackComponent).toBeTruthy();
    });

    it('should not display fallback component when not in dev mode', () => {
        // Set development mode to false
        dotcmsStore.$isDevMode.mockReturnValue(false);

        // Set UserComponent to null and UserNoComponent to a value
        component.$UserComponent.set(null);
        component.$UserNoComponent.set(Promise.resolve(MockComponent as Type<MockComponent>));

        spectator.detectChanges();

        const fallbackComponent = spectator.query('dotcms-fallback-component');
        expect(fallbackComponent).toBeFalsy();
    });

    it('should update style based on dev mode and content status', () => {
        // Set development mode to true
        dotcmsStore.$isDevMode.mockReturnValue(true);

        // Set haveContent to true
        component.$haveContent.set(false);

        spectator.detectChanges();

        expect(component.$style()).toEqual({ minHeight: '4rem' });
    });

    it('should handle user components from store in ngOnChanges', async () => {
        // Testing the implementation of setupComponents indirectly
        // Reset component signals
        component.$UserComponent.set(null);
        component.$UserNoComponent.set(null);

        // Call ngOnChanges which calls setupComponents internally
        component.ngOnChanges();

        // Verify the expected results
        const resolvedUserComponent = await component.$UserComponent();
        const resolvedNoComponent = await component.$UserNoComponent();
        expect(resolvedUserComponent).toBe(MockComponent);
        expect(resolvedNoComponent).toBe(MockComponent);
    });

    it('should set haveContent based on element rect in ngAfterViewInit', () => {
        // Testing checkContent method indirectly
        component.$haveContent.set(false);

        // Mock the contentletRef
        component.contentletRef = {
            nativeElement: {
                getBoundingClientRect: () => ({ height: 100 })
            }
        } as ElementRef;

        // Call ngAfterViewInit which calls checkContent internally
        component.ngAfterViewInit();

        // Verify haveContent was updated correctly
        expect(component.$haveContent()).toBe(true);
    });
});
