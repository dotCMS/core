// Stub the side panel so the flag-on "create new" branch can create it via `ViewContainerRef`
// without pulling in the real editor (and its module cycle). Placed before the imports so jest
// hoists it ahead of the dynamic `import()` the component performs. Kept as a real standalone
// component so `createComponent`/`setInput('data')`/`instance.closed` all work.
jest.mock(
    '../../../../components/dot-edit-content-side-panel/dot-edit-content-side-panel.component',
    () => {
        const { Component, input, output } = jest.requireActual('@angular/core');

        @Component({ selector: 'dot-edit-content-side-panel', standalone: true, template: '' })
        class MockDotEditContentSidePanelComponent {
            data = input(null);
            closed = output();
            saved = output();
        }

        return { DotEditContentSidePanelComponent: MockDotEditContentSidePanelComponent };
    }
);

import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    createFakeContentlet,
    createFakeLanguage,
    createFakeRelationshipField
} from '@dotcms/utils-testing';

import { DotRelationshipFieldComponent } from './dot-relationship-field.component';

// Resolves to the mock declared in the jest.mock above (same module path the component imports).
import { DotEditContentSidePanelComponent } from '../../../../components/dot-edit-content-side-panel/dot-edit-content-side-panel.component';
import { EDIT_CONTENT_HOST } from '../../../../services/host/edit-content-host.model';
import { DotEditContentStore } from '../../../../store/edit-content.store';
import { TableColumn } from '../../models/relationship.models';
import { RelationshipFieldStore } from '../../store/relationship-field.store';

// Renders as "English (en)" via LanguagePipe, matching the chip-text assertions.
const ENGLISH_LANGUAGE = createFakeLanguage({
    id: 1,
    language: 'English',
    languageCode: 'en',
    isoCode: 'en-us'
});

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
        lastChangeSource: jest.fn().mockReturnValue('load'),
        // `withFlags` slice — side panel off by default (empty map ⇒ create-new uses the dialog).
        flags: jest.fn().mockReturnValue({}),
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
                isCopyingLocale: jest.fn().mockReturnValue(false),
                contentlet: jest.fn().mockReturnValue(null),
                translationSourceInode: jest.fn().mockReturnValue(null)
            }),
            mockProvider(DialogService, {
                open: jest.fn()
            }),
            {
                provide: EDIT_CONTENT_HOST,
                useValue: {
                    inPlaceNavigation: false,
                    setContentTitle: jest.fn(),
                    addBreadcrumb: jest.fn(),
                    goToSavedContent: jest.fn(),
                    goToRestoredVersion: jest.fn(),
                    goToRelatedContent: jest.fn(),
                    goToCrumb: jest.fn()
                }
            }
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
            const localeHeader = spectator.query(byTestId('relationship-locale-header'));
            expect(localeHeader).toBeTruthy();
            expect(localeHeader.textContent.trim()).toContain(
                'dot.file.relationship.field.table.language'
            );
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
            const statusHeader = spectator.query(byTestId('relationship-status-header'));
            expect(statusHeader).toBeTruthy();
            expect(statusHeader).toHaveClass('text-right!');
            expect(statusHeader).not.toHaveClass('text-left');
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

        it('should open the existing-content dialog when the relate link is clicked', () => {
            const dialogService = spectator.inject(DialogService);
            spectator.click(byTestId('relationship-empty-relate-link'));
            expect(dialogService.open).toHaveBeenCalled();
        });

        it('wires the "New content" menu item to the create-new action', () => {
            // The menu item is the user-facing trigger; verify it calls the action (rather than
            // only exercising the method directly elsewhere).
            const createSpy = jest
                .spyOn(spectator.component, 'showCreateNewContentDialog')
                .mockResolvedValue();

            spectator.component.$menuItems()[1].command?.(undefined as never);

            expect(createSpy).toHaveBeenCalledTimes(1);
        });

        it('opens the create-new content in the centered dialog when the side panel flag is off', async () => {
            const dialogService = spectator.inject(DialogService);
            (dialogService.open as jest.Mock).mockClear();

            await spectator.component.showCreateNewContentDialog();

            expect(dialogService.open).toHaveBeenCalledTimes(1);
            const [, config] = (dialogService.open as jest.Mock).mock.calls[0];
            expect(config.data).toEqual(
                expect.objectContaining({ mode: 'new', contentTypeId: 'ct-1' })
            );
        });

        it('opens the create-new content in the side panel when the flag is on', async () => {
            const dialogService = spectator.inject(DialogService);
            (dialogService.open as jest.Mock).mockClear();
            storeMock.flags.mockReturnValue({
                [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: true
            });

            await spectator.component.showCreateNewContentDialog();
            spectator.detectChanges();

            // Flag on → the side panel is created imperatively, NOT the centered dialog.
            expect(dialogService.open).not.toHaveBeenCalled();
            expect(spectator.query('dot-edit-content-side-panel')).toBeTruthy();
        });

        it('falls back to the centered dialog when the flag slice has not resolved', async () => {
            const dialogService = spectator.inject(DialogService);
            (dialogService.open as jest.Mock).mockClear();
            // Empty slice = withFlags degraded on a failed config read, or a click before it
            // resolved. Either way the create-new action must not be a silent no-op: fall back to
            // the centered dialog (previous behavior).
            storeMock.flags.mockReturnValue({});

            await spectator.component.showCreateNewContentDialog();

            expect(dialogService.open).toHaveBeenCalledTimes(1);
            const [, config] = (dialogService.open as jest.Mock).mock.calls[0];
            expect(config.data).toEqual(
                expect.objectContaining({ mode: 'new', contentTypeId: 'ct-1' })
            );
        });

        it('destroys the side panel when it emits `closed`', async () => {
            storeMock.flags.mockReturnValue({
                [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: true
            });

            await spectator.component.showCreateNewContentDialog();
            spectator.detectChanges();

            const panel = spectator.query(DotEditContentSidePanelComponent);
            expect(panel).toBeTruthy();

            // Emitting `closed` must tear the panel down: its subscription calls #closeSidePanel,
            // which destroys the ComponentRef. A broken/missing subscription would leave it mounted.
            panel?.closed.emit();
            spectator.detectChanges();

            expect(spectator.query('dot-edit-content-side-panel')).toBeFalsy();
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

        it('should register the onChange/onTouched callbacks without throwing', () => {
            const onChangeSpy = jest.fn();
            const onTouchedSpy = jest.fn();

            expect(() => {
                spectator.component.registerOnChange(onChangeSpy);
                spectator.component.registerOnTouched(onTouchedSpy);
            }).not.toThrow();
        });

        it('should toggle the disabled signal via setDisabledState', () => {
            spectator.component.setDisabledState(true);
            expect(spectator.component.$isDisabled()).toBe(true);

            spectator.component.setDisabledState(false);
            expect(spectator.component.$isDisabled()).toBe(false);
        });
    });

    describe('Touched state on value sync', () => {
        // Dirty/touched is driven by the store's `lastChangeSource`: a user-driven
        // change (relate/remove/reorder) marks the control touched; a programmatic
        // 'load' sync (initial load / locale re-init) must NOT, so a required empty
        // field shows no validation error on render and the unsaved-changes guard
        // does not fire on a content the user never touched.
        let onChangeSpy: jest.Mock;
        let onTouchedSpy: jest.Mock;

        const setupWithSource = (source: 'load' | 'user') => {
            setup({
                data: jest.fn().mockReturnValue([]),
                paginatedData: jest.fn().mockReturnValue([]),
                totalPages: jest.fn().mockReturnValue(0),
                formattedRelationship: jest.fn().mockReturnValue(''),
                lastChangeSource: jest.fn().mockReturnValue(source)
            });

            onChangeSpy = jest.fn();
            onTouchedSpy = jest.fn();
            spectator.component.registerOnChange(onChangeSpy);
            spectator.component.registerOnTouched(onTouchedSpy);
        };

        it('should mark the control touched on a user-driven change', () => {
            setupWithSource('user');

            spectator.component.updateValueField('id-1');
            spectator.flushEffects();

            expect(onChangeSpy).toHaveBeenLastCalledWith('id-1');
            expect(onTouchedSpy).toHaveBeenCalled();
        });

        it('should NOT mark the control touched on a programmatic load sync', () => {
            setupWithSource('load');

            spectator.component.updateValueField('id-1');
            spectator.flushEffects();

            expect(onChangeSpy).toHaveBeenLastCalledWith('id-1');
            expect(onTouchedSpy).not.toHaveBeenCalled();
        });
    });

    describe('openRelated (navigate to related content)', () => {
        const CURRENT = { inode: 'current-inode', title: 'Current content' };
        let host: { goToRelatedContent: jest.Mock; goToCrumb: jest.Mock };
        let editStore: { contentlet: jest.Mock; translationSourceInode: jest.Mock };

        beforeEach(() => {
            setup();
            host = spectator.inject(EDIT_CONTENT_HOST) as never;
            editStore = spectator.inject(DotEditContentStore) as never;
            editStore.contentlet.mockReturnValue(CURRENT);
            editStore.translationSourceInode.mockReturnValue(null);
            // The mock's jest.fn is created once in the factory config and shared
            // across tests, so reset call counts here.
            host.goToRelatedContent.mockClear();
            host.goToCrumb.mockClear();
        });

        it('delegates to the host, seeding the current content as origin', () => {
            spectator.component.openRelated(
                buildItem({ inode: 'related-inode', title: 'Related content' })
            );

            expect(host.goToRelatedContent).toHaveBeenCalledWith(
                { inode: 'current-inode', title: 'Current content' },
                { inode: 'related-inode', title: 'Related content' }
            );
        });

        it('uses the translation source inode as origin when the current content has no inode', () => {
            // Locale switch to an untranslated locale: the new translation has no
            // inode, so the version we came from seeds the trail origin.
            editStore.contentlet.mockReturnValue({ inode: undefined, title: 'New translation' });
            editStore.translationSourceInode.mockReturnValue('source-inode');

            spectator.component.openRelated(
                buildItem({ inode: 'related-inode', title: 'Related content' })
            );

            // The origin is the SOURCE version (source-inode), not the current
            // translation, so its crumb is not relabeled with the translation's title —
            // the origin title is left empty and the source's cached title stands.
            expect(host.goToRelatedContent).toHaveBeenCalledWith(
                { inode: 'source-inode', title: '' },
                { inode: 'related-inode', title: 'Related content' }
            );
            expect(host.goToCrumb).not.toHaveBeenCalled();
        });

        it('starts a fresh trail when there is no inode and no translation source', () => {
            editStore.contentlet.mockReturnValue({ inode: undefined, title: 'New translation' });
            editStore.translationSourceInode.mockReturnValue(null);

            spectator.component.openRelated(buildItem({ inode: 'related-inode' }));

            expect(host.goToCrumb).toHaveBeenCalledWith('related-inode', ['related-inode']);
            expect(host.goToRelatedContent).not.toHaveBeenCalled();
        });

        it('is a no-op when the field is disabled (not navigable)', () => {
            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            spectator.component.openRelated(buildItem({ inode: 'related-inode' }));

            expect(host.goToRelatedContent).not.toHaveBeenCalled();
            expect(host.goToCrumb).not.toHaveBeenCalled();
        });

        it('is a no-op when navigating to the content already open (self-navigation)', () => {
            spectator.component.openRelated(buildItem({ inode: CURRENT.inode }));

            expect(host.goToRelatedContent).not.toHaveBeenCalled();
            expect(host.goToCrumb).not.toHaveBeenCalled();
        });

        it('is a no-op when the item has no inode', () => {
            spectator.component.openRelated(buildItem({ inode: undefined }));

            expect(host.goToRelatedContent).not.toHaveBeenCalled();
            expect(host.goToCrumb).not.toHaveBeenCalled();
        });
    });
});
