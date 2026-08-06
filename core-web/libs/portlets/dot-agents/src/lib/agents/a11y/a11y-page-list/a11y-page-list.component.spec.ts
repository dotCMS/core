import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { ActivatedRoute, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yPageListComponent } from './a11y-page-list.component';

import { StudioPageRow } from '../models/accessibility-studio.models';
import { A11yPageListStore } from '../store/a11y-page-list.store';

const MOCK_ROWS: StudioPageRow[] = [
    {
        identifier: 'id-1',
        title: 'About Us',
        path: '/about-us',
        type: 'htmlpageasset',
        languageId: 1,
        hostId: 'host-id-1',
        hostName: 'demo.dotcms.com',
        modDate: '04/09/2026',
        modUserName: 'Admin User',
        live: true
    },
    {
        identifier: 'id-2',
        title: 'Draft Page',
        path: '/draft',
        type: 'Blog',
        languageId: 1,
        hostId: 'host-id-1',
        hostName: 'demo.dotcms.com',
        modDate: '03/10/2026',
        modUserName: 'Admin User',
        live: false
    }
];

describe('DotA11yPageListComponent', () => {
    let spectator: Spectator<DotA11yPageListComponent>;

    const setFilter = jest.fn();
    const setPagination = jest.fn();
    const navigate = jest.fn();

    const storeMock = {
        pages: () => MOCK_ROWS,
        totalRecords: () => 2,
        page: () => 1,
        rows: () => 25,
        filter: () => '',
        pageListStatus: () => 'loaded',
        setFilter,
        setPagination
    };

    const createComponent = createComponentFactory({
        component: DotA11yPageListComponent,
        componentProviders: [{ provide: A11yPageListStore, useValue: storeMock }],
        providers: [
            { provide: Router, useValue: { navigate } },
            { provide: ActivatedRoute, useValue: {} },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.title': 'Accessibility Studio',
                    'accessibility.studio.pagelist.col.title': 'Title',
                    'accessibility.studio.pagelist.status.published': 'Published',
                    'accessibility.studio.pagelist.status.draft': 'Draft'
                })
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('renders a row per page', () => {
        expect(spectator.queryAll(byTestId('studio-page-row')).length).toBe(2);
    });

    it('renders the page title', () => {
        const titles = spectator.queryAll(byTestId('studio-page-title'));
        expect(titles[0]).toHaveText('About Us');
    });

    it('navigates to the page run route (deep link) when a row is clicked', () => {
        spectator.click(spectator.queryAll(byTestId('studio-page-row'))[0]);
        // Navigates to the page path as route segments relative to the page list —
        // the run screen then drives the store from the URL. Selection not set here.
        // MOCK_ROWS[0].path === '/about-us' → ['about-us'].
        expect(navigate).toHaveBeenCalledWith(
            ['about-us'],
            expect.objectContaining({ relativeTo: expect.anything() })
        );
    });

    it('debounces search input before calling setFilter', () => {
        jest.useFakeTimers();
        spectator.component.onSearch('contact');
        expect(setFilter).not.toHaveBeenCalled();
        jest.advanceTimersByTime(300);
        expect(setFilter).toHaveBeenCalledWith('contact');
        jest.useRealTimers();
    });
});
