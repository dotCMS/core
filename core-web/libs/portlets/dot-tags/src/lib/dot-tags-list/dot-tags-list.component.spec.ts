import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTagsListComponent } from './dot-tags-list.component';
import { DotTagsListStore } from './store/dot-tags-list.store';

const MOCK_TAGS: DotTag[] = [
    { id: '1', label: 'tag1', siteId: 'site1', siteName: 'Site 1', persona: false },
    { id: '2', label: 'tag2', siteId: 'site2', siteName: 'Site 2', persona: false }
];

describe('DotTagsListComponent', () => {
    let spectator: Spectator<DotTagsListComponent>;
    let store: InstanceType<typeof DotTagsListStore>;

    const createComponent = createComponentFactory({
        component: DotTagsListComponent,
        componentProviders: [
            mockProvider(DotTagsListStore, {
                tags: jest.fn().mockReturnValue(MOCK_TAGS),
                selectedTags: jest.fn().mockReturnValue(MOCK_TAGS),
                filter: jest.fn().mockReturnValue(''),
                page: jest.fn().mockReturnValue(1),
                rows: jest.fn().mockReturnValue(25),
                totalRecords: jest.fn().mockReturnValue(100),
                status: jest.fn().mockReturnValue('loaded'),
                sortField: jest.fn().mockReturnValue('tagname'),
                sortOrder: jest.fn().mockReturnValue('ASC'),
                setFilter: jest.fn(),
                setPagination: jest.fn(),
                setSort: jest.fn(),
                setSelectedTags: jest.fn(),
                openCreateDialog: jest.fn(),
                openEditDialog: jest.fn(),
                openImportDialog: jest.fn(),
                confirmDelete: jest.fn(),
                exportSelectedTags: jest.fn(),
                loadTags: jest.fn()
            }),
            DialogService,
            ConfirmationService
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent();
        store = spectator.inject(DotTagsListStore, true);
        jest.clearAllMocks();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('Search', () => {
        it('should debounce search by 300ms', () => {
            spectator.component.onSearch('test');
            jest.advanceTimersByTime(299);
            expect(store.setFilter).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);
            expect(store.setFilter).toHaveBeenCalledWith('test');
        });

        it('should reset debounce timer on rapid typing', () => {
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('ab');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('abc');
            jest.advanceTimersByTime(300);

            expect(store.setFilter).toHaveBeenCalledTimes(1);
            expect(store.setFilter).toHaveBeenCalledWith('abc');
        });
    });

    describe('Lazy Load', () => {
        it('should compute page correctly and call setPagination and setSort', () => {
            spectator.component.onLazyLoad({
                first: 50,
                rows: 25,
                sortField: 'tagname',
                sortOrder: 1
            });

            expect(store.setPagination).toHaveBeenCalledWith(3, 25);
            expect(store.setSort).toHaveBeenCalledWith('tagname', 'ASC');
        });

        it('should not call setSort when sortField is not provided', () => {
            spectator.component.onLazyLoad({ first: 0, rows: 25 });

            expect(store.setPagination).toHaveBeenCalledWith(1, 25);
            expect(store.setSort).not.toHaveBeenCalled();
        });

        it('should handle DESC sort order', () => {
            spectator.component.onLazyLoad({
                first: 0,
                rows: 25,
                sortField: 'tagname',
                sortOrder: -1
            });

            expect(store.setSort).toHaveBeenCalledWith('tagname', 'DESC');
        });
    });

    describe('Button Interactions', () => {
        it('should call openCreateDialog when Add Tag clicked', () => {
            const btnHost = spectator.query(byTestId('tag-add-btn'));
            const button = btnHost?.querySelector('button');
            spectator.click(button!);
            expect(store.openCreateDialog).toHaveBeenCalled();
        });

        it('should call openImportDialog when Import clicked', () => {
            const btnHost = spectator.query(byTestId('tag-import-btn'));
            const button = btnHost?.querySelector('button');
            spectator.click(button!);
            expect(store.openImportDialog).toHaveBeenCalled();
        });

        it('should call confirmDelete when Delete clicked', () => {
            const btnHost = spectator.query(byTestId('tag-delete-btn'));
            const button = btnHost?.querySelector('button');
            spectator.click(button!);
            expect(store.confirmDelete).toHaveBeenCalled();
        });

        it('should call exportSelectedTags when Export clicked', () => {
            const btnHost = spectator.query(byTestId('tag-export-btn'));
            const button = btnHost?.querySelector('button');
            spectator.click(button!);
            expect(store.exportSelectedTags).toHaveBeenCalled();
        });
    });

    describe('Row Click', () => {
        it('should call openEditDialog when tag row clicked', () => {
            spectator.detectChanges();
            const row = spectator.query(byTestId('tag-row'));
            spectator.click(row!);
            expect(store.openEditDialog).toHaveBeenCalled();
        });
    });
});
