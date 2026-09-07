import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import {
    DotCategoriesService,
    DotContentletService,
    DotMessageService,
    DotTagsService
} from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFieldFilterComponent } from './dot-field-filter.component';

import { DOT_FILTER_FACADE, DotFilterFacade } from '../../filter-facade.token';

const field = (overrides: Partial<DotCMSContentTypeField> = {}): DotCMSContentTypeField =>
    ({
        variable: 'aField',
        name: 'A Field',
        fieldType: 'Text',
        dataType: 'TEXT',
        values: '',
        ...overrides
    }) as DotCMSContentTypeField;

const relationshipField = (): DotCMSContentTypeField =>
    field({
        variable: 'author',
        name: 'Author',
        fieldType: 'Relationship',
        relationships: { velocityVar: 'Author.blogs', cardinality: 1, isParentField: true }
    } as Partial<DotCMSContentTypeField>);

/**
 * The chip on a surface that supplies **no** `DOT_RELATIONSHIP_PICKER` — the AssetPicker's case,
 * and the whole point of FR-020: an optional capability degrades one field type and nothing else.
 *
 * Its own file because the main spec's factory provides the capability at the component level, and
 * a component's providers cannot be un-provided once the TestBed is instantiated. The legacy-host
 * guard lives in a separate file for the same reason.
 */
describe('DotFieldFilterComponent without a relationship picker', () => {
    let spectator: Spectator<DotFieldFilterComponent>;

    const facade: DotFilterFacade = {
        getFilterValue: jest.fn(() => undefined),
        patchFilters: jest.fn(),
        removeFilter: jest.fn(),
        clearFilters: jest.fn(),
        $hasNonDefaultFilters: signal(false)
    };

    const createComponent = createComponentFactory({
        component: DotFieldFilterComponent,
        providers: [
            { provide: DOT_FILTER_FACADE, useValue: facade },
            mockProvider(DotTagsService),
            mockProvider(DotCategoriesService),
            mockProvider(DotContentletService, {
                getContentletByInode: jest.fn().mockReturnValue(of(null))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.field-filter.relationship.unavailable': 'Not available here',
                    'content-drive.field-filter.text.placeholder': 'Type to filter'
                })
            }
        ],
        detectChanges: false
    });

    afterEach(() => jest.clearAllMocks());

    describe('a Relationship field', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { field: relationshipField() } as never });
            spectator.detectChanges();
        });

        it('should still render the chip', () => {
            // Dropping it would leave a restored `us.*` value filtering with nothing on screen to
            // clear it.
            expect(spectator.query(byTestId('field-filter-chip-author'))).toBeTruthy();
        });

        it('should mark it as unavailable rather than letting it look ordinary', () => {
            // It ships an i18n key, so the editor is told why it cannot be opened here (FR-014e).
            expect(spectator.query(byTestId('field-filter-unavailable-author'))).toBeTruthy();
        });

        it('should do nothing when clicked, instead of opening a panel with no control in it', () => {
            spectator.click(spectator.query(byTestId('field-filter-chip-author')) as Element);
            spectator.detectChanges();

            expect(spectator.query('.p-popover', { root: true })).toBeNull();
        });
    });

    describe('every other field type', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: { field: field({ variable: 'body', fieldType: 'Text' }) } as never
            });
            spectator.detectChanges();
        });

        it('should not be marked unavailable', () => {
            // The degradation is scoped to Relationship — that is FR-020's whole claim.
            expect(spectator.query(byTestId('field-filter-unavailable-body'))).toBeNull();
        });

        it('should open and offer its control', () => {
            spectator.click(spectator.query(byTestId('field-filter-chip-body')) as Element);
            spectator.detectChanges();

            expect(spectator.query(byTestId('field-filter-text'), { root: true })).toBeTruthy();
        });
    });
});
