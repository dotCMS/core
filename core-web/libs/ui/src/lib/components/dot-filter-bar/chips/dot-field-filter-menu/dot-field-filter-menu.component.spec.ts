import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal, WritableSignal } from '@angular/core';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFieldFilterMenuComponent } from './dot-field-filter-menu.component';

import { DOT_FILTER_FACADE, DotFilterFacade } from '../../filter-facade.token';
import {
    DOT_FIELD_FILTER_HOST,
    DotFieldFilterHost
} from '../dot-field-filter/field-filter-host.token';

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
        field({ variable: 'raw', name: 'Raw', fieldType: 'JSON-Field' }), // eligible (text-fallback)
        field({ variable: 'meta', name: 'Meta', fieldType: 'Key-Value' }), // eligible (key/value)
        field({ variable: 'secret', name: 'Secret', searchable: false }), // excluded: not searchable
        field({ variable: 'notIndexed', name: 'Not Indexed', indexed: false }), // excluded: not indexed
        field({ variable: 'hidden', name: 'Hidden', fieldType: 'Hidden' }) // excluded: out-of-scope type
    ]
} as DotCMSContentType;

/** The surface, as the menu sees it: one facade for filter values, one host for the chips. */
type FilterFacadeMock = jest.Mocked<Pick<DotFilterFacade, 'getFilterValue'>>;
type FieldFilterHostMock = Omit<DotFieldFilterHost, '$activeFields' | '$fields'> & {
    $activeFields: WritableSignal<string[]>;
    $fields: WritableSignal<DotCMSContentTypeField[]>;
};

describe('DotFieldFilterMenuComponent', () => {
    let spectator: Spectator<DotFieldFilterMenuComponent>;
    let filters: FilterFacadeMock;
    let host: jest.Mocked<FieldFilterHostMock>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    const createComponent = createComponentFactory({
        component: DotFieldFilterMenuComponent,
        providers: [
            {
                provide: DOT_FILTER_FACADE,
                useFactory: (): FilterFacadeMock => ({
                    getFilterValue: jest.fn().mockReturnValue(undefined)
                })
            },
            {
                provide: DOT_FIELD_FILTER_HOST,
                useFactory: (): FieldFilterHostMock => ({
                    $activeFields: signal<string[]>([]),
                    $fields: signal<DotCMSContentTypeField[]>([]),
                    addField: jest.fn(),
                    setFields: jest.fn(),
                    clearFields: jest.fn()
                })
            },
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
        filters = spectator.inject(DOT_FILTER_FACADE) as FilterFacadeMock;
        host = spectator.inject(DOT_FIELD_FILTER_HOST) as jest.Mocked<FieldFilterHostMock>;
        contentTypeService = spectator.inject(DotContentTypeService, true);
    });

    afterEach(() => jest.clearAllMocks());

    const moreButton = () =>
        spectator.query(byTestId('field-filter-more-button'))?.querySelector('button');

    it('should disable the More button when no content type is selected', () => {
        filters.getFilterValue.mockReturnValue(undefined);
        spectator.detectChanges();

        expect(moreButton()?.disabled).toBe(true);
    });

    it('should disable the More button when more than one content type is selected', () => {
        filters.getFilterValue.mockReturnValue(['Blog', 'News']);
        spectator.detectChanges();

        expect(moreButton()?.disabled).toBe(true);
    });

    it('should enable the More button when exactly one content type is selected', () => {
        filters.getFilterValue.mockReturnValue(['Blog']);
        spectator.detectChanges();

        expect(moreButton()?.disabled).toBe(false);
    });

    it('should load only the eligible fields for the selected content type', () => {
        filters.getFilterValue.mockReturnValue(['blog']);
        spectator.detectChanges();

        expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
        // Only searchable + indexed + supported + non-title fields survive — including the
        // text-fallback (JSON) and Key/Value types, and excluding out-of-scope Hidden.
        expect(host.setFields).toHaveBeenCalledWith({
            eligible: [
                expect.objectContaining({ variable: 'body' }),
                expect.objectContaining({ variable: 'tags' }),
                expect.objectContaining({ variable: 'raw' }),
                expect.objectContaining({ variable: 'meta' })
            ],
            all: CONTENT_TYPE.fields
        });
    });

    it('should add a field as a chip when its option is clicked', () => {
        filters.getFilterValue.mockReturnValue(['blog']);
        host.$fields.set([field({ variable: 'body', name: 'Body' })]);
        spectator.detectChanges();

        spectator.click(moreButton() as HTMLElement);
        spectator.detectChanges();

        // The popover overlay is appended to the document body, so query from the root.
        const option = spectator.query(byTestId('field-filter-menu-option-body'), { root: true });
        spectator.click(option as Element);

        expect(host.addField).toHaveBeenCalledWith('body');
    });

    it('should not list a field that is already active', () => {
        filters.getFilterValue.mockReturnValue(['blog']);
        host.$fields.set([field({ variable: 'body', name: 'Body' })]);
        host.$activeFields.set(['body']);
        spectator.detectChanges();

        spectator.click(moreButton() as HTMLElement);
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('field-filter-menu-option-body'), { root: true })
        ).toBeNull();
        expect(spectator.query(byTestId('field-filter-menu-empty'), { root: true })).toBeTruthy();
    });

    it('should clear field filters when the active content type changes, but not on first load', () => {
        const contentType = signal<string[] | undefined>(['blog']);
        filters.getFilterValue.mockImplementation((key: string) =>
            key === 'contentType' ? contentType() : undefined
        );

        spectator.detectChanges();
        // First load resolves an active type but must not clear.
        expect(host.clearFields).not.toHaveBeenCalled();

        contentType.set(['news']);
        spectator.detectChanges();
        // Switching to a different type drops the previous type's field filters.
        expect(host.clearFields).toHaveBeenCalled();
    });

    describe('a failed field fetch (FR-015)', () => {
        beforeEach(() => {
            contentTypeService.getContentType.mockReturnValue(throwError(() => new Error('boom')));
            filters.getFilterValue.mockReturnValue(['blog']);
        });

        it('should report the failure through its output rather than handling it', () => {
            const reported = jest.fn();
            spectator.output('error').subscribe(reported);

            spectator.detectChanges();

            expect(reported).toHaveBeenCalledWith({
                messageKey: 'content-drive.field-filter.more.error'
            });
        });

        it('should degrade to an empty option list and stay interactive', () => {
            spectator.detectChanges();

            expect(host.setFields).toHaveBeenCalledWith({ eligible: [], all: [] });
            // Still enabled: a surface must never be left with a control it cannot click.
            expect(moreButton()?.disabled).toBe(false);
        });
    });

    it('should hand the raw field list to the surface, which decides what else to do with it', () => {
        const fields = [
            field({ variable: 'title', name: 'Title' }), // not listed
            field({ variable: 'summary', name: 'Summary', listed: true }),
            field({ variable: 'author', name: 'Author', listed: true })
        ];
        contentTypeService.getContentType.mockReturnValue(
            of({ id: 'blog', fields } as DotCMSContentType)
        );
        filters.getFilterValue.mockReturnValue(['blog']);

        spectator.detectChanges();

        // `all` is the untouched response: Content Drive mines it for its table's "Show In List"
        // columns, and the picker — which has no such table — ignores it.
        expect(host.setFields).toHaveBeenCalledWith(expect.objectContaining({ all: fields }));
    });

    it('should clear the published fields when the single content-type selection is removed', () => {
        const contentType = signal<string[] | undefined>(['blog']);
        filters.getFilterValue.mockImplementation((key: string) =>
            key === 'contentType' ? contentType() : undefined
        );

        spectator.detectChanges();

        contentType.set(undefined);
        spectator.detectChanges();

        expect(host.setFields).toHaveBeenLastCalledWith({ eligible: [], all: [] });
    });

    describe('the per-content-type cache (survives the move)', () => {
        it('should not refetch a content type it has already loaded', () => {
            const contentType = signal<string[] | undefined>(['blog']);
            filters.getFilterValue.mockImplementation((key: string) =>
                key === 'contentType' ? contentType() : undefined
            );

            spectator.detectChanges();
            contentType.set(['news']);
            spectator.detectChanges();
            contentType.set(['blog']);
            spectator.detectChanges();

            expect(contentTypeService.getContentType).toHaveBeenCalledTimes(2);
            expect(contentTypeService.getContentType).toHaveBeenNthCalledWith(1, 'blog');
            expect(contentTypeService.getContentType).toHaveBeenNthCalledWith(2, 'news');
        });
    });
});
