import { afterEach, beforeEach, describe, expect, it } from '@jest/globals';
import { createComponentFactory, Spectator } from '@openng/spectator/jest';

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

    const hostAttr = (name: string) =>
        spectator.debugElement.nativeElement.getAttribute(name) as string | null;

    beforeEach(() => {
        dotcmsStore = {
            $isDevMode: jest.fn().mockReturnValue(false),
            $isAnalyticsActive: jest.fn().mockReturnValue(false),
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

    describe('edit mode (UVE)', () => {
        beforeEach(() => {
            dotcmsStore.$isDevMode.mockReturnValue(true);
            spectator.detectChanges();
        });

        it('should emit the full editor attribute set on the host', () => {
            expect(hostAttr('data-dot-object')).toBe('contentlet');
            expect(hostAttr('data-dot-identifier')).toBe('test-contentlet-id');
            expect(hostAttr('data-dot-basetype')).toBe('test-basetype');
            expect(hostAttr('data-dot-title')).toBe('Test Contentlet');
            expect(hostAttr('data-dot-inode')).toBe('test-inode');
            expect(hostAttr('data-dot-type')).toBe('test-content-type');
            expect(hostAttr('data-dot-container')).toBe(JSON.stringify(component.containerData));
            expect(hostAttr('data-dot-on-number-of-pages')).toBe('1');
        });

        it('should emit data-dot-style-properties when the contentlet has style properties', () => {
            const contentletWithStyleProperties = {
                ...mockContentlet,
                dotStyleProperties: { 'font-size': 20, 'font-family': 'Arial' }
            };

            spectator.setInput('contentlet', contentletWithStyleProperties);
            spectator.detectChanges();

            expect(hostAttr('data-dot-style-properties')).toBe(
                JSON.stringify(contentletWithStyleProperties.dotStyleProperties)
            );
        });

        it('should not emit data-dot-style-properties when the contentlet has none', () => {
            expect(hostAttr('data-dot-style-properties')).toBeNull();
        });
    });

    describe('live mode without analytics', () => {
        beforeEach(() => {
            dotcmsStore.$isDevMode.mockReturnValue(false);
            dotcmsStore.$isAnalyticsActive.mockReturnValue(false);
            spectator.detectChanges();
        });

        it('should not emit any data-dot-* attributes on the host', () => {
            const dotAttrs = Array.from(spectator.debugElement.nativeElement.attributes)
                .map((attr) => (attr as Attr).name)
                .filter((name) => name.startsWith('data-dot'));

            expect(dotAttrs).toEqual([]);
        });
    });

    describe('live mode with analytics active', () => {
        beforeEach(() => {
            dotcmsStore.$isDevMode.mockReturnValue(false);
            dotcmsStore.$isAnalyticsActive.mockReturnValue(true);
            spectator.detectChanges();
        });

        it('should emit only the minimal analytics attributes', () => {
            expect(hostAttr('data-dot-identifier')).toBe('test-contentlet-id');
            expect(hostAttr('data-dot-inode')).toBe('test-inode');
            expect(hostAttr('data-dot-title')).toBe('Test Contentlet');
            expect(hostAttr('data-dot-type')).toBe('test-content-type');
            expect(hostAttr('data-dot-basetype')).toBe('test-basetype');
        });

        it('should not emit editor-only attributes', () => {
            expect(hostAttr('data-dot-object')).toBeNull();
            expect(hostAttr('data-dot-container')).toBeNull();
            expect(hostAttr('data-dot-on-number-of-pages')).toBeNull();
            expect(hostAttr('data-dot-style-properties')).toBeNull();
        });
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
        spectator.detectChanges();
        dotcmsStore.$isDevMode.mockReturnValue(true);

        component.$UserComponent.set(null);
        component.$UserNoComponent.set(Promise.resolve(MockComponent as Type<MockComponent>));
        spectator.detectChanges();

        const fallbackComponent = spectator.query('dotcms-fallback-component');
        expect(fallbackComponent).toBeTruthy();
    });

    it('should display fallback component when in dev mode, UserComponent is not available and UserNoComponent is not available', () => {
        spectator.detectChanges();
        dotcmsStore.$isDevMode.mockReturnValue(true);

        component.$UserComponent.set(null);
        component.$UserNoComponent.set(null);
        spectator.detectChanges();

        const fallbackComponent = spectator.query('dotcms-fallback-component');
        expect(fallbackComponent).toBeTruthy();
    });

    it('should not display fallback component when not in dev mode', () => {
        dotcmsStore.$isDevMode.mockReturnValue(false);

        component.$UserComponent.set(null);
        component.$UserNoComponent.set(Promise.resolve(MockComponent as Type<MockComponent>));

        spectator.detectChanges();

        const fallbackComponent = spectator.query('dotcms-fallback-component');
        expect(fallbackComponent).toBeFalsy();
    });

    it('should update style based on dev mode and content status', () => {
        dotcmsStore.$isDevMode.mockReturnValue(true);

        component.$haveContent.set(false);

        spectator.detectChanges();

        expect(component.$style()).toEqual({ minHeight: '4rem' });
    });

    it('should handle user components from store in ngOnChanges', async () => {
        component.$UserComponent.set(null);
        component.$UserNoComponent.set(null);

        component.ngOnChanges();

        const resolvedUserComponent = await component.$UserComponent();
        const resolvedNoComponent = await component.$UserNoComponent();
        expect(resolvedUserComponent).toBe(MockComponent);
        expect(resolvedNoComponent).toBe(MockComponent);
    });

    it('should set haveContent based on element rect in ngAfterViewInit', () => {
        component.$haveContent.set(false);

        component.contentletRef = {
            nativeElement: {
                getBoundingClientRect: () => ({ height: 100 })
            }
        } as ElementRef;

        component.ngAfterViewInit();

        expect(component.$haveContent()).toBe(true);
    });
});
