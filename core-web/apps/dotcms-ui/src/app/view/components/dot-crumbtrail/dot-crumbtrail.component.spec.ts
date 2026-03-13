import { createComponentFactory, mockProvider, Spectator, byTestId } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';

import { MenuItem } from 'primeng/api';

import { DotCurrentUserService, DotSiteService, DotSystemConfigService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';

describe('DotCrumbtrailComponent', () => {
    let spectator: Spectator<DotCrumbtrailComponent>;
    let store: InstanceType<typeof GlobalStore>;

    const createComponent = createComponentFactory({
        component: DotCrumbtrailComponent,
        imports: [DotCollapseBreadcrumbComponent],
        providers: [
            GlobalStore,
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService),
            mockProvider(DotCurrentUserService)
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(GlobalStore);
        // Reset breadcrumbs before each test
        patchState(unprotected(store), { breadcrumbs: [] });
    });

    it('should have breadcrumb parent container', () => {
        spectator.detectChanges();
        const breadcrumbParent = spectator.query(byTestId('breadcrumb-crumbs'));
        expect(breadcrumbParent).toBeTruthy();
    });

    it('should use dot-collapse-breadcrumb component', () => {
        spectator.detectChanges();
        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu).toBeTruthy();
    });

    it('should display collapsed breadcrumbs when multiple items are provided', () => {
        const crumbs = [
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' },
            { label: 'Last', url: '/last' }
        ];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' }
        ]);
    });

    it('should display last breadcrumb label when multiple items are provided', () => {
        const crumbs = [
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' },
            { label: 'Last', url: '/last' }
        ];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Last');
    });

    it('should display empty collapsed breadcrumbs when only one item is provided', () => {
        const crumbs = [{ label: 'Single Item', url: '/single' }];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([]);
    });

    it('should display single item as last breadcrumb when only one item is provided', () => {
        const crumbs = [{ label: 'Single Item', url: '/single' }];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Single Item');
    });

    it('should not display breadcrumb title when no items are provided', () => {
        const crumbs: MenuItem[] = [];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast).toBeFalsy();
    });

    it('should display empty collapsed breadcrumbs when no items are provided', () => {
        const crumbs: MenuItem[] = [];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([]);
    });

    it('should handle breadcrumbs with target and url properties', () => {
        const crumbs = [
            { label: 'First', target: '_self', url: '/first' },
            { label: 'Second', target: '_blank', url: '/second' },
            { label: 'Last', url: '/last' }
        ];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'First', target: '_self', url: '/first' },
            { label: 'Second', target: '_blank', url: '/second' }
        ]);

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Last');
    });

    it('should update collapsed breadcrumbs when service emits new data', () => {
        const initialCrumbs = [
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' }
        ];

        patchState(unprotected(store), { breadcrumbs: initialCrumbs });
        spectator.detectChanges();

        let breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([{ label: 'First', url: '/first' }]);

        const updatedCrumbs = [
            { label: 'Home', url: '/home' },
            { label: 'Section', url: '/section' },
            { label: 'Page', url: '/page' }
        ];

        patchState(unprotected(store), { breadcrumbs: updatedCrumbs });
        spectator.detectChanges();

        breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'Home', url: '/home' },
            { label: 'Section', url: '/section' }
        ]);

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Page');
    });

    it('should handle breadcrumbs with empty label', () => {
        const crumbs = [
            { label: 'First', url: '/first' },
            { label: '', url: '/empty' },
            { label: 'Last', url: '/last' }
        ];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'First', url: '/first' },
            { label: '', url: '/empty' }
        ]);

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Last');
    });

    it('should handle breadcrumbs with null label', () => {
        const crumbs = [
            { label: 'First', url: '/first' },
            { label: null, url: '/null' },
            { label: 'Last', url: '/last' }
        ];

        patchState(unprotected(store), { breadcrumbs: crumbs });
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'First', url: '/first' },
            { label: null, url: '/null' }
        ]);

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Last');
    });
});
