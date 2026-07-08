import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, Subject } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCategoriesService, DotMessageService, DotTagsService } from '@dotcms/data-access';
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

    const createComponent = createComponentFactory({
        component: DotContentDriveFieldFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn()
            }),
            mockProvider(DotTagsService, {
                getSuggestions: jest.fn().mockReturnValue(of([{ label: 'angular' }]))
            }),
            mockProvider(DotCategoriesService, {
                getChildrenPaginated: jest
                    .fn()
                    .mockReturnValue(of({ entity: [{ categoryName: 'News', inode: 'i1' }] })),
                getCategoriesPaginated: jest.fn().mockReturnValue(of({ entity: [] }))
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
    });

    afterEach(() => jest.clearAllMocks());

    /** Opens the chip popover by clicking the chip (any field-filter chip). */
    const openPopover = () => {
        spectator.click(spectator.query('[data-testid^="field-filter-chip-"]') as Element);
        spectator.detectChanges();
    };

    describe('control per field type', () => {
        const cases: { fieldType: string; values?: string; testId: string }[] = [
            { fieldType: 'Text', testId: 'field-filter-text' },
            { fieldType: 'Select', values: 'A|a', testId: 'field-filter-select' },
            { fieldType: 'Radio', values: 'A|a', testId: 'field-filter-radio' },
            { fieldType: 'Multi-Select', values: 'A|a', testId: 'field-filter-multi-select' },
            { fieldType: 'Checkbox', values: 'A|a\r\nB|b', testId: 'field-filter-checkbox' },
            { fieldType: 'Checkbox', values: '|true', testId: 'field-filter-binary' },
            { fieldType: 'Date', testId: 'field-filter-date' }
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

    describe('chip', () => {
        it('should render the field name as the chip title', () => {
            spectator.setInput('field', field({ name: 'My Field' }));
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-title'))?.textContent?.trim()).toBe('My Field');
        });

        it('should clear the value (keep the chip) when the chip is removed', () => {
            spectator.setInput('field', field({ variable: 'body' }));
            spectator.detectChanges();

            spectator.triggerEventHandler('dot-chip-filter', 'removed', undefined);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.body': '' });
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

        it('should store the selected contentlet identifiers on close', () => {
            const onClose = new Subject<DotCMSContentlet[]>();
            dialogService.open.mockReturnValue({ onClose } as never);
            spectator.setInput('field', relationshipField());
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('field-filter-chip-author')) as Element);
            onClose.next([
                { identifier: 'id-1', inode: 'inode-1', title: 'First' } as DotCMSContentlet
            ]);

            expect(store.patchFilters).toHaveBeenCalledWith({ 'us.author': 'id-1' });
        });
    });
});
