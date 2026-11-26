import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import {
    createComponentFactory,
    mockProvider,
    Spectator,
    byTestId
} from '@openng/spectator/vitest';

import { MenuItem } from 'primeng/api';

import {
    DotCurrentUserService,
    DotMessageService,
    DotSiteService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { DotTitleStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';
import { DOT_SYSTEM_CONFIG_SERVICE_MOCK } from '@dotcms/utils-testing';

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
            mockProvider(DotSystemConfigService, DOT_SYSTEM_CONFIG_SERVICE_MOCK),
            mockProvider(DotCurrentUserService),
            mockProvider(DotMessageService, { get: (key: string) => key })
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

    describe('title status chip', () => {
        const publishedStatus: DotTitleStatus = {
            state: { live: true, working: true, hasLiveVersion: true }
        };

        const withTitle = (title = 'My content') => {
            patchState(unprotected(store), {
                breadcrumbs: [
                    { label: 'Home', disabled: true },
                    { label: title, url: '/content/abc' }
                ]
            });
        };

        it('should not show a chip when no page set a title status', () => {
            withTitle();
            spectator.detectChanges();

            expect(spectator.query(byTestId('title-status-chip'))).toBeFalsy();
        });

        it('should show the status badge right after the title', () => {
            withTitle();
            store.setTitleStatus(publishedStatus);
            spectator.detectChanges();

            const title = spectator.query(byTestId('breadcrumb-title'));
            const chip = spectator.query(byTestId('title-status-chip'));

            expect(chip).toBeTruthy();
            expect(chip?.textContent?.trim()).toBe('Published');
            const position = title?.compareDocumentPosition(chip as Node) ?? 0;

            expect(position & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        });

        it('should read "New" for content that has not been saved', () => {
            withTitle('New Blog');
            store.setTitleStatus({ state: null });
            spectator.detectChanges();

            expect(spectator.query(byTestId('title-status-chip'))?.textContent?.trim()).toBe('New');
        });

        it('should update the chip when the status changes without a reload', () => {
            withTitle();
            store.setTitleStatus(publishedStatus);
            spectator.detectChanges();

            store.setTitleStatus({ state: { live: false, working: true, hasLiveVersion: false } });
            spectator.detectChanges();

            expect(spectator.query(byTestId('title-status-chip'))?.textContent?.trim()).toBe(
                'Draft'
            );
        });

        it('should remove the chip when the status is cleared', () => {
            withTitle();
            store.setTitleStatus(publishedStatus);
            spectator.detectChanges();

            store.clearTitleStatus();
            spectator.detectChanges();

            expect(spectator.query(byTestId('title-status-chip'))).toBeFalsy();
        });

        it('should not show a chip when there is no title to sit next to', () => {
            patchState(unprotected(store), { breadcrumbs: [] });
            store.setTitleStatus(publishedStatus);
            spectator.detectChanges();

            expect(spectator.query(byTestId('title-status-chip'))).toBeFalsy();
        });

        it('should let a long title shrink while the chip keeps its size', () => {
            withTitle('A'.repeat(200));
            store.setTitleStatus(publishedStatus);
            spectator.detectChanges();

            const title = spectator.query(byTestId('breadcrumb-title'));
            const chip = spectator.query(byTestId('title-status-chip'));

            expect(title?.classList.contains('min-w-0')).toBe(true);
            expect(title?.classList.contains('truncate-text')).toBe(true);
            expect(chip?.classList.contains('shrink-0')).toBe(true);
        });
    });
});
