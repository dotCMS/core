import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAccessibilityStudioPickerComponent } from './dot-accessibility-studio-picker.component';

import { StudioPageRow } from '../models/accessibility-studio.models';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

const MOCK_ROWS: StudioPageRow[] = [
    {
        identifier: 'id-1',
        title: 'About Us',
        path: '/about-us',
        type: 'htmlpageasset',
        languageId: 1,
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
        hostName: 'demo.dotcms.com',
        modDate: '03/10/2026',
        modUserName: 'Admin User',
        live: false
    }
];

describe('DotAccessibilityStudioPickerComponent', () => {
    let spectator: Spectator<DotAccessibilityStudioPickerComponent>;

    const openPage = jest.fn();
    const setFilter = jest.fn();
    const setPagination = jest.fn();

    const storeMock = {
        pages: () => MOCK_ROWS,
        totalRecords: () => 2,
        page: () => 1,
        rows: () => 25,
        filter: () => '',
        pickerStatus: () => 'loaded',
        openPage,
        setFilter,
        setPagination
    };

    const createComponent = createComponentFactory({
        component: DotAccessibilityStudioPickerComponent,
        componentProviders: [
            { provide: AccessibilityStudioStore, useValue: storeMock }
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.title': 'Accessibility Studio',
                    'accessibility.studio.picker.col.title': 'Title',
                    'accessibility.studio.picker.status.published': 'Published',
                    'accessibility.studio.picker.status.draft': 'Draft'
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

    it('calls store.openPage when a row is clicked', () => {
        spectator.click(spectator.queryAll(byTestId('studio-page-row'))[0]);
        expect(openPage).toHaveBeenCalledWith(MOCK_ROWS[0]);
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
