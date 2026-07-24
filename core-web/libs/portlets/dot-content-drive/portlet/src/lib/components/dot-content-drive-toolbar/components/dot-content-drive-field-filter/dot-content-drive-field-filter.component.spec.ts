import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotCategoriesService,
    DotContentletService,
    DotMessageService,
    DotTagsService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveFieldFilterComponent } from './dot-content-drive-field-filter.component';

import { DEBOUNCE_TIME } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const field = (overrides: Partial<DotCMSContentTypeField> = {}): DotCMSContentTypeField =>
    ({
        variable: 'aField',
        name: 'A Field',
        fieldType: 'Text',
        dataType: 'TEXT',
        values: '',
        ...overrides
    }) as DotCMSContentTypeField;

describe('DotContentDriveFieldFilterComponent', () => {
    let spectator: Spectator<DotContentDriveFieldFilterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let dialogService: SpyObject<DialogService>;
    let contentletService: SpyObject<DotContentletService>;
    let categoriesService: SpyObject<DotCategoriesService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveFieldFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn()
            }),
            mockProvider(DotTagsService, {
                getTagsPaginated: jest.fn().mockReturnValue(of({ entity: [{ label: 'angular' }] }))
            }),
            mockProvider(DotCategoriesService, {
                getChildrenPaginated: jest
                    .fn()
                    .mockReturnValue(of({ entity: [{ categoryName: 'News', inode: 'i1' }] })),
                getCategoriesPaginated: jest.fn().mockReturnValue(of({ entity: [] })),
                getCategory: jest.fn().mockReturnValue(of({ inode: 'i1', categoryName: 'News' }))
            }),
            mockProvider(DotContentletService, {
                getContentletByInode: jest
                    .fn()
                    .mockReturnValue(of({ identifier: 'id-1', inode: 'inode-1', title: 'First' }))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ true: 'True', false: 'False' })
            }
        ],
        componentProviders: [mockProvider(DialogService, { open: jest.fn() })],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({ props: { field: field() } as never });
        store = spectator.inject(DotContentDriveStore, true);
        dialogService = spectator.inject(DialogService, true);
        contentletService = spectator.inject(DotContentletService, true);
        categoriesService = spectator.inject(DotCategoriesService, true);
    });

    afterEach(() => jest.clearAllMocks());

    /** Opens the chip popover by clicking the chip (any field-filter chip). */
    const openPopover = () => {
        spectator.click(spectator.query('[data-testid^="field-filter-chip-"]') as Element);
        spectator.detectChanges();
    };

    /**
     * Fires an Angular output/`ngModel` event on a control inside the popover. The popover content
     * is body-appended, so the debug element is resolved from the root (`{ root: true }`).
     */
    const emitOnControl = (testId: string, event: string, payload: unknown) =>
        spectator.triggerEventHandler(`[data-testid="${testId}"]`, event, payload, {
            root: true
        });

    describe('control per field type', () => {
        const cases: { fieldType: string; values?: string; testId: string }[] = [
            { fieldType: 'Text', testId: 'field-filter-text' },
            { fieldType: 'Select', values: 'A|a', testId: 'field-filter-select' },
            { fieldType: 'Radio', values: 'A|a', testId: 'field-filter-radio' },
            { fieldType: 'Multi-Select', values: 'A|a', testId: 'field-filter-multi-select' },
            { fieldType: 'Checkbox', values: 'A|a\r\nB|b', testId: 'field-filter-checkbox' },
            { fieldType: 'Checkbox', values: '|true', testId: 'field-filter-binary' },
            { fieldType: 'Tag', testId: 'field-filter-lazy-multiselect' },
            { fieldType: 'Category', testId: 'field-filter-lazy-multiselect' },
            { fieldType: 'Date', testId: 'field-filter-date' },
            { fieldType: 'Date-and-Time', testId: 'field-filter-datetime' },
            { fieldType: 'Time', testId: 'field-filter-time' },
            // Text-fallback types render the plain text control (contains).
            { fieldType: 'JSON-Field', testId: 'field-filter-text' },
            { fieldType: 'Story-Block', testId: 'field-filter-text' },
            { fieldType: 'Custom-Field', testId: 'field-filter-text' },
            { fieldType: 'Binary', testId: 'field-filter-text' },
            // Key/Value renders its own single input.
            { fieldType: 'Key-Value', testId: 'field-filter-key-value' }
        ];

        cases.forEach(({ fieldType, values, testId }) => {
            it(`should render the ${testId} control for a ${fieldType} field`, () => {
                spectator.setInput('field', field({ fieldType, values }));
                spectator.detectChanges();
                openPopover();

                expect(spectator.query(byTestId(testId), { root: true })).toBeTruthy();
            });
        });
    });

    describe('text value', () => {
        it('should signal a numeric input mode for a whole-number text field', () => {
            spectator.setInput('field', field({ fieldType: 'Text', dataType: 'INTEGER' }));
            spectator.detectChanges();
            openPopover();

            const input = spectator.query(byTestId('field-filter-text'), { root: true });
            expect(input?.getAttribute('inputmode')).toBe('numeric');
        });

        it('should signal a decimal input mode for a decimal text field', () => {
            spectator.setInput('field', field({ fieldType: 'Text', dataType: 'FLOAT' }));
            spectator.detectChanges();
            openPopover();

            const input = spectator.query(byTestId('field-filter-text'), { root: true });
            expect(input?.getAttribute('inputmode')).toBe('decimal');
        });

        it('should use a filename-specific placeholder for a Binary field', () => {
            spectator.setInput('field', field({ fieldType: 'Binary' }));
            spectator.detectChanges();
            openPopover();

            // MockDotMessageService echoes the key, so the resolved key is asserted directly.
            const input = spectator.query(byTestId('field-filter-text'), { root: true });
            expect(input?.getAttribute('placeholder')).toBe(
                'content-drive.field-filter.binary.placeholder'
            );
        });

        it('should use a JSON-specific placeholder for a JSON field', () => {
            spectator.setInput('field', field({ fieldType: 'JSON-Field' }));
            spectator.detectChanges();
            openPopover();

            const input = spectator.query(byTestId('field-filter-text'), { root: true });
            expect(input?.getAttribute('placeholder')).toBe(
                'content-drive.field-filter.json.placeholder'
            );
        });

        it('should use a text-content placeholder for a Story Block field', () => {
            spectator.setInput('field', field({ fieldType: 'Story-Block' }));
            spectator.detectChanges();
            openPopover();

            const input = spectator.query(byTestId('field-filter-text'), { root: true });
            expect(input?.getAttribute('placeholder')).toBe(
                'content-drive.field-filter.story-block.placeholder'
            );
        });

        it('should use the generic placeholder for a text-fallback field without its own copy', () => {
            spectator.setInput('field', field({ fieldType: 'Custom-Field' }));
            spectator.detectChanges();
            openPopover();

            const input = spectator.query(byTestId('field-filter-text'), { root: true });
            expect(input?.getAttribute('placeholder')).toBe(
                'content-drive.field-filter.text.placeholder'
            );
        });

        describe('debounce', () => {
            beforeEach(() => jest.useFakeTimers());
            afterEach(() => jest.useRealTimers());

            it('should patch the filter with the typed value (debounced)', () => {
                spectator.setInput('field', field({ variable: 'body', fieldType: 'Text' }));
                spectator.detectChanges();
                openPopover();

                const input = spectator.query(byTestId('field-filter-text'), { root: true });
                spectator.typeInElement('hello', input as HTMLInputElement);
                jest.advanceTimersByTime(DEBOUNCE_TIME);

                expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': 'hello' });
            });
        });
    });

    describe('key-value', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should render the input and the shorthand hint', () => {
            spectator.setInput('field', field({ variable: 'meta', fieldType: 'Key-Value' }));
            spectator.detectChanges();
            openPopover();

            expect(
                spectator.query(byTestId('field-filter-key-value'), { root: true })
            ).toBeTruthy();
            expect(
                spectator.query(byTestId('field-filter-key-value-hint'), { root: true })
            ).toBeTruthy();
        });

        it('should store the literal input verbatim (translation happens at payload build)', () => {
            spectator.setInput('field', field({ variable: 'meta', fieldType: 'Key-Value' }));
            spectator.detectChanges();
            openPopover();

            const input = spectator.query(byTestId('field-filter-key-value'), { root: true });
            spectator.typeInElement('color:red', input as HTMLInputElement);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            // The chip/URL keep the user's text; the `:`→`_` join is applied downstream.
            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.meta': 'color:red' });
        });
    });

    describe('chip', () => {
        it('should render the field name as the chip title', () => {
            spectator.setInput('field', field({ name: 'My Field' }));
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-title'))?.textContent?.trim()).toBe('My Field');
        });

        it('should clear the value (keep the chip) when the chip is removed', () => {
            jest.useFakeTimers();
            spectator.setInput('field', field({ variable: 'body' }));
            spectator.detectChanges();

            spectator.triggerEventHandler('dot-chip-filter', 'removed', undefined);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': '' });
            jest.useRealTimers();
        });
    });

    describe('time range', () => {
        it('should render two independent time pickers (from / to) for a Time field', () => {
            spectator.setInput('field', field({ fieldType: 'Time' }));
            spectator.detectChanges();
            openPopover();

            // Two independent pickers, not a single range control (PrimeNG has no timeOnly range).
            expect(
                spectator.query(byTestId('field-filter-time-from'), { root: true })
            ).toBeTruthy();
            expect(spectator.query(byTestId('field-filter-time-to'), { root: true })).toBeTruthy();
            expect(spectator.query(byTestId('field-filter-date'), { root: true })).toBeNull();
        });

        it('should show an inline error for an inverted (from after to) range', () => {
            store.getFilterValue.mockReturnValue(
                '2024-01-01T17:00:00.000Z,2024-01-01T09:00:00.000Z'
            );
            spectator.setInput('field', field({ fieldType: 'Time' }));
            spectator.detectChanges();
            openPopover();

            expect(
                spectator.query(byTestId('field-filter-time-error'), { root: true })
            ).toBeTruthy();
        });

        it('should not show the error for a valid (from before to) range', () => {
            store.getFilterValue.mockReturnValue(
                '2024-01-01T09:00:00.000Z,2024-01-01T17:00:00.000Z'
            );
            spectator.setInput('field', field({ fieldType: 'Time' }));
            spectator.detectChanges();
            openPopover();

            expect(spectator.query(byTestId('field-filter-time-error'), { root: true })).toBeNull();
        });
    });

    describe('lazy multi-select (Tag / Category)', () => {
        it('should resolve a stored Category inode to its name on load (without opening the dropdown)', () => {
            store.getFilterValue.mockReturnValue('i1');
            spectator.setInput(
                'field',
                field({ variable: 'cat', fieldType: 'Category', values: 'root' })
            );
            spectator.detectChanges();
            spectator.detectChanges();

            expect(categoriesService.getCategory).toHaveBeenCalledWith('i1');
            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('News');
        });
    });

    describe('relationship', () => {
        const relationshipField = () =>
            field({
                variable: 'author',
                fieldType: 'Relationship',
                relationships: { velocityVar: 'Author.blogs', cardinality: 1, isParentField: true }
            } as Partial<DotCMSContentTypeField>);

        it('should open the picker dialog when the chip is clicked', () => {
            dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);
            spectator.setInput('field', relationshipField());
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('field-filter-chip-author')) as Element);

            expect(dialogService.open).toHaveBeenCalled();
        });

        it('should resolve the stored identifier to its inode and preselect it after a reload', () => {
            store.getFilterValue.mockReturnValue('id-1');
            dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);
            spectator.setInput('field', relationshipField());
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('field-filter-chip-author')) as Element);

            expect(contentletService.getContentletByInode).toHaveBeenCalledWith('id-1');
            expect(dialogService.open).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    data: expect.objectContaining({ currentItemsIds: ['inode-1'] })
                })
            );
        });

        it('should show the resolved contentlet title in the chip after a reload', () => {
            store.getFilterValue.mockReturnValue('id-1');
            spectator.setInput('field', relationshipField());
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('First');
        });

        it('should store the selected contentlet identifiers on close', () => {
            jest.useFakeTimers();
            const onClose = new Subject<DotCMSContentlet[]>();
            dialogService.open.mockReturnValue({ onClose } as never);
            spectator.setInput('field', relationshipField());
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('field-filter-chip-author')) as Element);
            onClose.next([
                { identifier: 'id-1', inode: 'inode-1', title: 'First' } as DotCMSContentlet
            ]);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.author': 'id-1' });
            jest.useRealTimers();
        });
    });

    describe('time range interaction', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        // Local Dates + toISOString keep the assertions timezone-independent (the component
        // serializes with the same toISOString the test computes the expected value with).
        const seededFrom = new Date(2024, 0, 1, 17, 0, 0);
        const seededTo = new Date(2024, 0, 1, 9, 0, 0);

        const openInvertedTimeRange = () => {
            store.getFilterValue.mockReturnValue(
                `${seededFrom.toISOString()},${seededTo.toISOString()}`
            );
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Time' }));
            spectator.detectChanges();
            openPopover();
        };

        it('should not patch while an edit keeps the range inverted', () => {
            openInvertedTimeRange();

            // Still inverted: from 17:00 is after the new to 08:00.
            emitOnControl('field-filter-time-to', 'ngModelChange', new Date(2024, 0, 1, 8, 0, 0));
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).not.toHaveBeenCalled();
        });

        it('should patch the corrected range once an edit makes it valid', () => {
            openInvertedTimeRange();

            const correctedTo = new Date(2024, 0, 1, 18, 0, 0);
            emitOnControl('field-filter-time-to', 'ngModelChange', correctedTo);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledTimes(1);
            expect(store.patchFilters).toHaveBeenCalledWith({
                'us.body': `${seededFrom.toISOString()},${correctedTo.toISOString()}`
            });
        });

        it('should patch the from/to bounds driven through the two pickers', () => {
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Time' }));
            spectator.detectChanges();
            openPopover();

            const from = new Date(2024, 0, 1, 9, 0, 0);
            const to = new Date(2024, 0, 1, 17, 0, 0);
            emitOnControl('field-filter-time-from', 'ngModelChange', from);
            emitOnControl('field-filter-time-to', 'ngModelChange', to);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledTimes(1);
            expect(store.patchFilters).toHaveBeenCalledWith({
                'us.body': `${from.toISOString()},${to.toISOString()}`
            });
        });
    });

    describe('date-and-time interaction', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        // Existing bounds so the merge (date-part vs time-part) is deterministic.
        const seededFrom = new Date(2024, 0, 10, 8, 0, 0);
        const seededTo = new Date(2024, 0, 20, 18, 0, 0);

        const openWithSeededRange = () => {
            store.getFilterValue.mockReturnValue(
                `${seededFrom.toISOString()},${seededTo.toISOString()}`
            );
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Date-and-Time' }));
            spectator.detectChanges();
            openPopover();
        };

        it('should merge the picked date part into the existing time bounds', () => {
            openWithSeededRange();

            emitOnControl('field-filter-datetime-dates', 'ngModelChange', [
                new Date(2024, 2, 5),
                new Date(2024, 2, 25)
            ]);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            // Dates change to Mar 5 / Mar 25, but the seeded times (08:00 / 18:00) are kept.
            const expectedFrom = new Date(2024, 2, 5, 8, 0, 0);
            const expectedTo = new Date(2024, 2, 25, 18, 0, 0);
            expect(store.patchFilters).toHaveBeenCalledTimes(1);
            expect(store.patchFilters).toHaveBeenCalledWith({
                'us.body': `${expectedFrom.toISOString()},${expectedTo.toISOString()}`
            });
        });

        it('should merge the picked from-time into the existing from date', () => {
            openWithSeededRange();

            emitOnControl(
                'field-filter-datetime-from-time',
                'ngModelChange',
                new Date(2024, 5, 15, 10, 30, 0)
            );
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            // Time changes to 10:30, the seeded from date (Jan 10) is kept; to bound unchanged.
            const expectedFrom = new Date(2024, 0, 10, 10, 30, 0);
            expect(store.patchFilters).toHaveBeenCalledWith({
                'us.body': `${expectedFrom.toISOString()},${seededTo.toISOString()}`
            });
        });

        it('should merge the picked to-time into the existing to date', () => {
            openWithSeededRange();

            emitOnControl(
                'field-filter-datetime-to-time',
                'ngModelChange',
                new Date(2024, 5, 15, 20, 45, 0)
            );
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            const expectedTo = new Date(2024, 0, 20, 20, 45, 0);
            expect(store.patchFilters).toHaveBeenCalledWith({
                'us.body': `${seededFrom.toISOString()},${expectedTo.toISOString()}`
            });
        });
    });

    describe('$timeRangeInvalid for Date-and-Time (full-instant comparison)', () => {
        const seedRange = (from: Date, to: Date) => {
            store.getFilterValue.mockReturnValue(`${from.toISOString()},${to.toISOString()}`);
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Date-and-Time' }));
            spectator.detectChanges();
            openPopover();
        };

        it('should show the error when the dates are inverted even if the time-of-day matches', () => {
            // Same time-of-day (12:00) but from is a LATER date → invalid by full instant.
            seedRange(new Date(2024, 2, 10, 12, 0, 0), new Date(2024, 2, 5, 12, 0, 0));

            expect(
                spectator.query(byTestId('field-filter-time-error'), { root: true })
            ).toBeTruthy();
        });

        it('should not show the error when the full instant is valid despite an inverted time-of-day', () => {
            // From has a later time-of-day (20:00 > 08:00) but an earlier date → valid by instant.
            seedRange(new Date(2024, 2, 5, 20, 0, 0), new Date(2024, 2, 10, 8, 0, 0));

            expect(spectator.query(byTestId('field-filter-time-error'), { root: true })).toBeNull();
        });
    });

    describe('debounce coalescing', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should patch once with the final value for rapid changes within the window', () => {
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Text' }));
            spectator.detectChanges();
            openPopover();

            emitOnControl('field-filter-text', 'ngModelChange', 'a');
            emitOnControl('field-filter-text', 'ngModelChange', 'ab');
            emitOnControl('field-filter-text', 'ngModelChange', 'abc');
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledTimes(1);
            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': 'abc' });
        });
    });

    describe('date range (plain)', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        const openDateField = () => {
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Date' }));
            spectator.detectChanges();
            openPopover();
        };

        it('should patch the comma-joined ISO range', () => {
            openDateField();

            const from = new Date(2024, 0, 1);
            const to = new Date(2024, 0, 31);
            emitOnControl('field-filter-date', 'ngModelChange', [from, to]);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({
                'us.body': `${from.toISOString()},${to.toISOString()}`
            });
        });

        it('should patch an empty value when the range is cleared', () => {
            openDateField();

            emitOnControl('field-filter-date', 'ngModelChange', null);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': '' });
        });
    });

    describe('single- and multi-value selection', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should patch the selected value for a Select field', () => {
            spectator.setInput(
                'field',
                field({ variable: 'body', fieldType: 'Select', values: 'A|a\r\nB|b' })
            );
            spectator.detectChanges();
            openPopover();

            emitOnControl('field-filter-select', 'ngModelChange', 'a');
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': 'a' });
        });

        it('should patch an empty value when a Select is deselected', () => {
            spectator.setInput(
                'field',
                field({ variable: 'body', fieldType: 'Select', values: 'A|a\r\nB|b' })
            );
            spectator.detectChanges();
            openPopover();

            emitOnControl('field-filter-select', 'ngModelChange', null);
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': '' });
        });

        it('should patch the selected value for a Radio field', () => {
            spectator.setInput(
                'field',
                field({ variable: 'body', fieldType: 'Radio', values: 'A|a\r\nB|b' })
            );
            spectator.detectChanges();
            openPopover();

            emitOnControl('field-filter-radio', 'ngModelChange', 'b');
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': 'b' });
        });

        it('should patch the serialized comma list for a Multi-Select field', () => {
            spectator.setInput(
                'field',
                field({ variable: 'body', fieldType: 'Multi-Select', values: 'A|a\r\nB|b' })
            );
            spectator.detectChanges();
            openPopover();

            emitOnControl('field-filter-multi-select', 'ngModelChange', ['a', 'b']);
            emitOnControl('field-filter-multi-select', 'onChange', {});
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': 'a,b' });
        });
    });

    describe('lazy selection (Tag / Category)', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('should patch the serialized values emitted by the lazy multi-select', () => {
            spectator.setInput('field', field({ variable: 'body', fieldType: 'Tag' }));
            spectator.detectChanges();
            openPopover();

            spectator.triggerEventHandler(
                'dot-content-drive-lazy-multiselect',
                'selectionChange',
                [
                    { label: 'angular', value: 'angular' },
                    { label: 'nx', value: 'nx' }
                ],
                { root: true }
            );
            jest.advanceTimersByTime(DEBOUNCE_TIME);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': 'angular,nx' });
        });
    });

    describe('resolution error fallbacks', () => {
        it('should fall back to the neutral count when a relationship lookup fails', () => {
            contentletService.getContentletByInode.mockReturnValue(
                throwError(() => new Error('boom'))
            );
            store.getFilterValue.mockReturnValue('id-1');

            expect(() => {
                spectator.setInput(
                    'field',
                    field({
                        variable: 'author',
                        fieldType: 'Relationship',
                        relationships: {
                            velocityVar: 'Author.blogs',
                            cardinality: 1,
                            isParentField: true
                        }
                    } as Partial<DotCMSContentTypeField>)
                );
                spectator.detectChanges();
                spectator.detectChanges();
            }).not.toThrow();

            expect(contentletService.getContentletByInode).toHaveBeenCalledWith('id-1');
            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain(
                'content-drive.field-filter.selected-count'
            );
        });

        it('should fall back to the raw inode when a category lookup fails', () => {
            categoriesService.getCategory.mockReturnValue(throwError(() => new Error('boom')));
            store.getFilterValue.mockReturnValue('i1');

            expect(() => {
                spectator.setInput(
                    'field',
                    field({ variable: 'cat', fieldType: 'Category', values: 'root' })
                );
                spectator.detectChanges();
                spectator.detectChanges();
            }).not.toThrow();

            expect(categoriesService.getCategory).toHaveBeenCalledWith('i1');
            expect(spectator.query(byTestId('chip-values'))?.textContent).toContain('i1');
        });
    });
});
