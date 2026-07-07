import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveFieldFilterMenuComponent } from './dot-content-drive-field-filter-menu.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const field = (overrides: Partial<DotCMSContentTypeField> = {}): DotCMSContentTypeField =>
    ({
        variable: 'aField',
        name: 'A Field',
        fieldType: 'Text',
        dataType: 'TEXT',
        values: '',
        searchable: true,
        indexed: true,
        ...overrides
    }) as DotCMSContentTypeField;

// A content type whose fields exercise every eligibility rule.
const CONTENT_TYPE: DotCMSContentType = {
    id: 'blog',
    fields: [
        field({ variable: 'title', name: 'Title' }), // excluded: title field
        field({ variable: 'body', name: 'Body', fieldType: 'Text' }), // eligible
        field({ variable: 'tags', name: 'Tags', fieldType: 'Tag' }), // eligible
        field({ variable: 'secret', name: 'Secret', searchable: false }), // excluded: not searchable
        field({ variable: 'notIndexed', name: 'Not Indexed', indexed: false }), // excluded: not indexed
        field({ variable: 'blob', name: 'Blob', fieldType: 'Binary' }) // excluded: out-of-scope type
    ]
} as DotCMSContentType;

describe('DotContentDriveFieldFilterMenuComponent', () => {
    let spectator: Spectator<DotContentDriveFieldFilterMenuComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveFieldFilterMenuComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                userSearchableFields: signal<DotCMSContentTypeField[]>([]),
                userSearchableActive: signal<string[]>([]),
                addUserSearchableField: jest.fn(),
                setUserSearchableFields: jest.fn(),
                clearUserSearchableFilters: jest.fn()
            }),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ],
        componentProviders: [
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of(CONTENT_TYPE))
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        contentTypeService = spectator.inject(DotContentTypeService, true);
    });

    afterEach(() => jest.clearAllMocks());

    const moreButton = () =>
        spectator.query(byTestId('field-filter-more-button'))?.querySelector('button');

    it('should disable the More button when no content type is selected', () => {
        store.getFilterValue.mockReturnValue(undefined);
        spectator.detectChanges();

        expect(moreButton()?.disabled).toBe(true);
    });

    it('should disable the More button when more than one content type is selected', () => {
        store.getFilterValue.mockReturnValue(['Blog', 'News']);
        spectator.detectChanges();

        expect(moreButton()?.disabled).toBe(true);
    });

    it('should enable the More button when exactly one content type is selected', () => {
        store.getFilterValue.mockReturnValue(['Blog']);
        spectator.detectChanges();

        expect(moreButton()?.disabled).toBe(false);
    });

    it('should load only the eligible fields for the selected content type', () => {
        store.getFilterValue.mockReturnValue(['blog']);
        spectator.detectChanges();

        expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
        // Only searchable + indexed + supported + non-title fields survive.
        expect(store.setUserSearchableFields).toHaveBeenCalledWith([
            expect.objectContaining({ variable: 'body' }),
            expect.objectContaining({ variable: 'tags' })
        ]);
    });

    it('should add a field as a chip when its option is clicked', () => {
        store.getFilterValue.mockReturnValue(['blog']);
        store.userSearchableFields.set([field({ variable: 'body', name: 'Body' })]);
        spectator.detectChanges();

        spectator.click(moreButton() as HTMLElement);
        spectator.detectChanges();

        // The popover overlay is appended to the document body, so query from the root.
        const option = spectator.query(byTestId('field-filter-menu-option-body'), { root: true });
        spectator.click(option as Element);

        expect(store.addUserSearchableField).toHaveBeenCalledWith('body');
    });

    it('should not list a field that is already active', () => {
        store.getFilterValue.mockReturnValue(['blog']);
        store.userSearchableFields.set([field({ variable: 'body', name: 'Body' })]);
        store.userSearchableActive.set(['body']);
        spectator.detectChanges();

        spectator.click(moreButton() as HTMLElement);
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('field-filter-menu-option-body'), { root: true })
        ).toBeNull();
        expect(spectator.query(byTestId('field-filter-menu-empty'), { root: true })).toBeTruthy();
    });
});
