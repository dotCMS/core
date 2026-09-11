import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Mocked, vi } from 'vitest';

import { signal } from '@angular/core';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DOT_FIELD_FILTER_HOST, DotFieldFilterHost } from '@dotcms/ui';

import { provideContentDriveFieldFilterHost } from './content-drive-field-filter-host';
import { DotContentDriveStore } from './dot-content-drive.store';

const field = (overrides: Partial<DotCMSContentTypeField> = {}): DotCMSContentTypeField =>
    ({
        variable: 'aField',
        name: 'A Field',
        fieldType: 'Text',
        ...overrides
    }) as DotCMSContentTypeField;

/**
 * The Content Drive side of the {@link DOT_FIELD_FILTER_HOST} seam.
 *
 * Most of it is delegation, but one thing is not: the `listed` split that feeds the results
 * table's "Show In List" columns. That lived in the field-filter menu — and was covered by its
 * spec — until the menu became shared; `@dotcms/ui` has no results table, so the split moved here
 * and its old test moved with the menu into a shape that only checks the raw list crosses the
 * seam. This is the missing half.
 */
describe('provideContentDriveFieldFilterHost', () => {
    let spectator: SpectatorService<unknown>;
    let host: DotFieldFilterHost;
    let store: Mocked<
        Pick<
            InstanceType<typeof DotContentDriveStore>,
            | 'addUserSearchableField'
            | 'setUserSearchableFields'
            | 'setShowInListFields'
            | 'clearUserSearchableFilters'
        >
    >;

    const activeFields = signal<string[]>([]);
    const fields = signal<DotCMSContentTypeField[]>([]);

    const createService = createServiceFactory({
        service: class {},
        providers: [
            mockProvider(DotContentDriveStore, {
                userSearchableActive: activeFields,
                userSearchableFields: fields,
                addUserSearchableField: vi.fn(),
                setUserSearchableFields: vi.fn(),
                setShowInListFields: vi.fn(),
                clearUserSearchableFilters: vi.fn()
            })
        ]
    });

    beforeEach(() => {
        activeFields.set([]);
        fields.set([]);
        spectator = createService({ providers: [provideContentDriveFieldFilterHost()] });
        host = spectator.inject(DOT_FIELD_FILTER_HOST);
        store = spectator.inject(DotContentDriveStore, true) as never;
    });

    afterEach(() => vi.clearAllMocks());

    it('should expose the store signals the chips read', () => {
        activeFields.set(['body']);
        fields.set([field({ variable: 'body' })]);

        expect(host.$activeFields()).toEqual(['body']);
        expect(host.$fields()).toEqual([expect.objectContaining({ variable: 'body' })]);
    });

    it('should add a chip through the store', () => {
        host.addField('body');

        expect(store.addUserSearchableField).toHaveBeenCalledWith('body');
    });

    it('should clear field filters through the store', () => {
        host.clearFields();

        expect(store.clearUserSearchableFilters).toHaveBeenCalled();
    });

    describe('publishing one field fetch', () => {
        const eligible = [field({ variable: 'body' })];
        const all = [
            field({ variable: 'title' }),
            field({ variable: 'summary', listed: true }),
            field({ variable: 'author', listed: true })
        ];

        it('should hand the eligible fields to the chips', () => {
            host.setFields({ eligible, all });

            expect(store.setUserSearchableFields).toHaveBeenCalledWith(eligible);
        });

        it('should mine the raw list for the results table Show In List columns', () => {
            // Only `listed` fields become extra columns, and the split belongs to this portlet:
            // the AssetPicker has no such table, which is why the shared menu hands over both
            // lists instead of doing this itself.
            host.setFields({ eligible, all });

            expect(store.setShowInListFields).toHaveBeenCalledWith([
                expect.objectContaining({ variable: 'summary' }),
                expect.objectContaining({ variable: 'author' })
            ]);
        });

        it('should clear both when the fetch produced nothing', () => {
            host.setFields({ eligible: [], all: [] });

            expect(store.setUserSearchableFields).toHaveBeenCalledWith([]);
            expect(store.setShowInListFields).toHaveBeenCalledWith([]);
        });
    });
});
