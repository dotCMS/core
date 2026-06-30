import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { DotRelationshipFieldComponent } from './dot-relationship-field.component';

import { DotEditContentStore } from '../../../../store/edit-content.store';
import { TableColumn } from '../../models/relationship.models';
import { RelationshipFieldStore } from '../../store/relationship-field.store';

const ENGLISH_LANGUAGE: DotLanguage = {
    id: 1,
    language: 'English',
    languageCode: 'en',
    countryCode: 'US',
    country: 'United States',
    isoCode: 'en-us'
};

const LANGUAGE_COLUMN: TableColumn = {
    nameField: 'language',
    header: 'Language',
    type: 'language'
};

const STATUS_COLUMN: TableColumn = {
    nameField: 'status',
    header: 'Status',
    type: 'status'
};

const TITLE_COLUMN: TableColumn = {
    nameField: 'title',
    header: 'Title',
    type: 'title'
};

const FIELD_MOCK = createFakeRelationshipField({
    variable: 'relationshipField'
});

const buildItem = (overrides: Partial<DotCMSContentlet> = {}): DotCMSContentlet =>
    createFakeContentlet({
        title: 'Related item',
        inode: 'inode-1',
        identifier: 'id-1',
        language: ENGLISH_LANGUAGE,
        ...overrides
    });

describe('DotRelationshipFieldComponent', () => {
    let spectator: Spectator<DotRelationshipFieldComponent>;

    // i18n mock returns the key itself so header/empty-state assertions are deterministic.
    const messageServiceMock = {
        get: jest.fn((key: string) => key)
    };

    const createStoreMock = (overrides: Record<string, unknown> = {}) => ({
        data: jest.fn().mockReturnValue([buildItem()]),
        paginatedData: jest.fn().mockReturnValue([buildItem()]),
        columns: jest.fn().mockReturnValue([TITLE_COLUMN, LANGUAGE_COLUMN, STATUS_COLUMN]),
        staticColumns: jest.fn().mockReturnValue(2),
        totalPages: jest.fn().mockReturnValue(1),
        pagination: jest.fn().mockReturnValue({ offset: 0, currentPage: 1, rowsPerPage: 6 }),
        showThumbnail: jest.fn().mockReturnValue(false),
        isDisabledCreateNewContent: jest.fn().mockReturnValue(false),
        isNewEditorEnabled: jest.fn().mockReturnValue(true),
        selectionMode: jest.fn().mockReturnValue('multiple'),
        contentType: jest.fn().mockReturnValue({ id: 'ct-1' }),
        formattedRelationship: jest.fn().mockReturnValue('id-1'),
        initialize: jest.fn(),
        setData: jest.fn(),
        deleteItem: jest.fn(),
        reorderData: jest.fn(),
        nextPage: jest.fn(),
        previousPage: jest.fn(),
        ...overrides
    });

    let storeMock: ReturnType<typeof createStoreMock>;

    const createComponent = createComponentFactory({
        component: DotRelationshipFieldComponent,
        detectChanges: false,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, messageServiceMock),
            mockProvider(DotEditContentStore, {
                contentType: jest.fn().mockReturnValue(null),
                currentLocale: jest.fn().mockReturnValue(null),
                isCopyingLocale: jest.fn().mockReturnValue(false)
            }),
            mockProvider(DialogService, {
                open: jest.fn()
            })
        ]
    });

    const setup = (storeOverrides: Record<string, unknown> = {}) => {
        storeMock = createStoreMock(storeOverrides);
        spectator = createComponent({
            providers: [{ provide: RelationshipFieldStore, useValue: storeMock }],
            props: {
                field: FIELD_MOCK,
                contentlet: buildItem(),
                hasError: false,
                isRequired: false
            }
        });
        spectator.detectChanges();
    };

    describe('Locales column', () => {
        beforeEach(() => setup());

        it('should render the Locales header using the table language key', () => {
            const headers = spectator.queryAll('th');
            const headerTexts = headers.map((h) => h.textContent.trim());
            expect(headerTexts).toContain('dot.file.relationship.field.table.language');
        });

        it('should render the locale value as a p-tag, not plain text', () => {
            const localeTag = spectator.query(byTestId('relationship-locale-tag'));
            expect(localeTag).toBeTruthy();
            expect(localeTag.textContent).toContain('English');
        });
    });

    describe('Status column header alignment', () => {
        beforeEach(() => setup());

        it('should right-align the status header so it lines up with the chips', () => {
            const statusHeader = spectator
                .queryAll('th')
                .find((h) => h.textContent.includes('dot.file.relationship.field.table.status'));
            expect(statusHeader).toBeTruthy();
            expect(statusHeader.className).toContain('text-right!');
            expect(statusHeader.className).not.toContain('text-left');
        });
    });

    describe('Empty state', () => {
        beforeEach(() =>
            setup({
                data: jest.fn().mockReturnValue([]),
                paginatedData: jest.fn().mockReturnValue([]),
                totalPages: jest.fn().mockReturnValue(0),
                formattedRelationship: jest.fn().mockReturnValue('')
            })
        );

        it('should render the empty-state message and relate link', () => {
            const emptyState = spectator.query(byTestId('relationship-field-empty'));
            expect(emptyState).toBeTruthy();
            expect(emptyState.textContent).toContain('dot.file.relationship.field.empty.message');

            const relateLink = spectator.query(byTestId('relationship-empty-relate-link'));
            expect(relateLink).toBeTruthy();
            expect(relateLink.textContent.trim()).toContain(
                'dot.file.relationship.field.empty.relate.link'
            );
        });

        it('should call showExistingContentDialog when the relate link is clicked', () => {
            const dialogSpy = jest.spyOn(spectator.component, 'showExistingContentDialog');
            spectator.click(byTestId('relationship-empty-relate-link'));
            expect(dialogSpy).toHaveBeenCalled();
        });
    });

    describe('Disabled state', () => {
        beforeEach(() => {
            setup({
                data: jest.fn().mockReturnValue([]),
                paginatedData: jest.fn().mockReturnValue([]),
                totalPages: jest.fn().mockReturnValue(0),
                formattedRelationship: jest.fn().mockReturnValue('')
            });
            spectator.component.setDisabledState(true);
            spectator.detectChanges();
        });

        it('should hide the relate link when the field is disabled', () => {
            expect(spectator.query(byTestId('relationship-empty-relate-link'))).toBeFalsy();
        });

        it('should not render the suffix when disabled, leaving only the base message', () => {
            const emptyState = spectator.query(byTestId('relationship-field-empty'));
            expect(emptyState.textContent).toContain('dot.file.relationship.field.empty.message');
            // The suffix lives inside the same @if(!isDisabled) block as the link,
            // so the disabled state must not render "or click the + button.".
            expect(emptyState.textContent).not.toContain(
                'dot.file.relationship.field.empty.message.suffix'
            );
        });
    });

    describe('Horizontal scrolling', () => {
        beforeEach(() => setup());

        it('should render a PrimeNG scrollable table so extra columns are not clipped', () => {
            const table = spectator.query(byTestId('relationship-field-table'));
            expect(table).toBeTruthy();
            expect(table.classList).toContain('p-datatable-scrollable');
        });
    });

    describe('Summary template', () => {
        it('should not render hint or error text inside the table summary', () => {
            setup({ totalPages: jest.fn().mockReturnValue(1) });

            const hint = spectator.query(byTestId(`hint-${FIELD_MOCK.variable}`));
            expect(hint).toBeFalsy();
        });
    });

    describe('Form control contract', () => {
        beforeEach(() => setup());

        it('should keep the ControlValueAccessor methods intact', () => {
            expect(spectator.component.writeValue).toBeDefined();
            expect(spectator.component.registerOnChange).toBeDefined();
            expect(spectator.component.registerOnTouched).toBeDefined();
            expect(spectator.component.setDisabledState).toBeDefined();
        });
    });

    describe('Touched state on value sync', () => {
        // The component constructor performs the initial programmatic value sync
        // (which must NOT mark the control touched). These tests assert that any
        // value sync AFTER that initial one — i.e. a genuine user-driven change such
        // as relating/removing content — does mark the control touched.
        let onChangeSpy: jest.Mock;
        let onTouchedSpy: jest.Mock;

        beforeEach(() => {
            // Empty data so formattedRelationship resolves to '' (the required-empty case).
            setup({
                data: jest.fn().mockReturnValue([]),
                paginatedData: jest.fn().mockReturnValue([]),
                totalPages: jest.fn().mockReturnValue(0),
                formattedRelationship: jest.fn().mockReturnValue('')
            });

            onChangeSpy = jest.fn();
            onTouchedSpy = jest.fn();
            spectator.component.registerOnChange(onChangeSpy);
            spectator.component.registerOnTouched(onTouchedSpy);
        });

        it('should mark the control touched on a user-driven change after init', () => {
            spectator.component.updateValueField('id-1');
            spectator.flushEffects();

            expect(onChangeSpy).toHaveBeenLastCalledWith('id-1');
            expect(onTouchedSpy).toHaveBeenCalled();
        });
    });
});
