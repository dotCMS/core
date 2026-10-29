import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Component, input, reflectComponentType, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { StyleEditorFormSchema } from '@dotcms/types/internal';
import { MockDotMessageService, createFakeContentlet } from '@dotcms/utils-testing';

import { DotUveEditPanelComponent } from './dot-uve-edit-panel.component';

import { EDIT_ACTION_PAYLOAD_MOCK } from '../../../shared/mocks';
import { ContentletPayload, SelectedContentlet } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../../../store/features/editor/models';
import { DotUveContentletQuickEditComponent } from '../dot-uve-contentlet-quick-edit/dot-uve-contentlet-quick-edit.component';
import { DotUveStyleEditorEmptyStateComponent } from '../dot-uve-palette/components/dot-uve-style-editor-empty-state/dot-uve-style-editor-empty-state.component';
import { DotUveStyleEditorFormComponent } from '../dot-uve-palette/components/dot-uve-style-editor-form/dot-uve-style-editor-form.component';

const MESSAGES = {
    'uve.toolbar.content.editor': 'Content Editor',
    'uve.toolbar.style.editor': 'Style Editor',
    'uve.contentlet.no.edit.permission': "You don't have permission to edit this content."
};

const SELECTED: SelectedContentlet = {
    bounds: { x: 0, y: 0, width: 0, height: 0 },
    payload: {
        ...EDIT_ACTION_PAYLOAD_MOCK,
        contentlet: {
            ...(EDIT_ACTION_PAYLOAD_MOCK.contentlet as ContentletPayload),
            contentType: 'Banner'
        }
    }
};

/** What the store hands quick edit for the selected contentlet. */
const createEditData = (contentlet = createFakeContentlet({ contentType: 'Banner' })) => ({
    container: SELECTED.payload.container,
    contentlet
});

const STYLE_SCHEMA = { contentType: 'Banner', sections: [] } as StyleEditorFormSchema;

/** The style form has its own spec; here it only shows which schema it was given. */
@Component({ selector: 'dot-uve-style-editor-form', template: '' })
class DotUveStyleEditorFormStubComponent {
    readonly schema = input<StyleEditorFormSchema>();
}

/** The empty state has its own spec; here it only shows which content type it was given. */
@Component({ selector: 'dot-uve-style-editor-empty-state', template: '' })
class DotUveStyleEditorEmptyStateStubComponent {
    readonly contentTypeVar = input<string>('');
}

/**
 * The part of `UVEStore` the panel and quick edit read and write. The tab setter writes back, as
 * the real store's does, so a click on a tab can be followed through to what the panel shows.
 */
function createUVEStoreMock() {
    const editorEditPanelTab = signal(UVE_EDIT_PANEL_TABS.QUICK_EDIT);

    return {
        editorEditPanelOpen: signal(true),
        editorEditPanelTab,
        editorSetEditPanelTab: vi.fn((tab: UVE_EDIT_PANEL_TABS) => editorEditPanelTab.set(tab)),
        editorCanEditStyles: signal(true),
        editorSelected: signal<SelectedContentlet | null>(SELECTED),
        $editorContentletEditData: signal(createEditData()),
        editorCanEditSelectedContentlet: signal(true),
        $editorIsIframeLoading: signal(false),
        $styleSchema: signal<StyleEditorFormSchema | undefined>(undefined),
        // Read by quick edit, which talks to the store itself.
        contentTypeCache: signal({}),
        loadContentType: vi.fn()
    };
}

describe('DotUveEditPanelComponent', () => {
    let spectator: Spectator<DotUveEditPanelComponent>;
    let store: ReturnType<typeof createUVEStoreMock>;

    const createComponent = createComponentFactory({
        component: DotUveEditPanelComponent,
        overrideComponents: [
            [
                DotUveEditPanelComponent,
                {
                    remove: {
                        imports: [
                            DotUveStyleEditorFormComponent,
                            DotUveStyleEditorEmptyStateComponent
                        ]
                    },
                    add: {
                        imports: [
                            DotUveStyleEditorFormStubComponent,
                            DotUveStyleEditorEmptyStateStubComponent
                        ]
                    }
                }
            ]
        ],
        providers: [
            { provide: UVEStore, useFactory: () => store },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ],
        detectChanges: false
    });

    const quickEdit = () => spectator.query(DotUveContentletQuickEditComponent);
    const styleForm = () => spectator.query(DotUveStyleEditorFormStubComponent);
    const styleEmptyState = () => spectator.query(DotUveStyleEditorEmptyStateStubComponent);

    const openStyleTab = () => {
        store.editorEditPanelTab.set(UVE_EDIT_PANEL_TABS.STYLE_EDITOR);
        spectator.detectChanges();
    };

    beforeEach(() => {
        store = createUVEStoreMock();
        spectator = createComponent();
    });

    it('should have no inputs or outputs', () => {
        const mirror = reflectComponentType(DotUveEditPanelComponent);

        expect(mirror?.inputs).toEqual([]);
        expect(mirror?.outputs).toEqual([]);
    });

    describe('Open state', () => {
        const body = () => spectator.query(byTestId('edit-panel-body'));

        it('should show at full width, with a border on its left, while the store holds the panel open', () => {
            spectator.detectChanges();

            expect(spectator.element).toHaveClass('w-[clamp(360px,20vw,500px)]');
            expect(spectator.element).toHaveClass('border-l');
            expect(spectator.element).not.toHaveClass('w-0');
            expect(body()).toBeTruthy();
            expect(body()).not.toHaveClass('translate-x-full');
        });

        it('should narrow to nothing and slide its body out while the store holds the panel closed', () => {
            store.editorEditPanelOpen.set(false);
            spectator.detectChanges();

            expect(spectator.element).toHaveClass('w-0');
            expect(spectator.element).not.toHaveClass('w-[clamp(360px,20vw,500px)]');
            expect(spectator.element).not.toHaveClass('border-l');
            expect(body()).toHaveClass('translate-x-full');
        });

        it('should open again when the store opens the panel', () => {
            store.editorEditPanelOpen.set(false);
            spectator.detectChanges();

            store.editorEditPanelOpen.set(true);
            spectator.detectChanges();

            expect(spectator.element).toHaveClass('w-[clamp(360px,20vw,500px)]');
            expect(spectator.element).toHaveClass('border-l');
            expect(body()).toBeTruthy();
            expect(body()).not.toHaveClass('translate-x-full');
        });
    });

    describe('tabs', () => {
        it('should show the quick-edit and style tabs when the page allows style editing', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-panel-quick-edit-tab'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-panel-style-editor-tab'))).toBeTruthy();
        });

        it('should show only the quick-edit tab when the page does not allow style editing', () => {
            store.editorCanEditStyles.set(false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-panel-quick-edit-tab'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-panel-style-editor-tab'))).toBeNull();
        });

        it('should open on the quick-edit tab when the store holds it', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('edit-panel-quick-edit-tab'))).toHaveAttribute(
                'aria-selected',
                'true'
            );
            expect(quickEdit()).toBeTruthy();
            expect(styleEmptyState()).toBeNull();
        });

        it('should open on the style tab when the store holds it', () => {
            openStyleTab();

            expect(spectator.query(byTestId('edit-panel-style-editor-tab'))).toHaveAttribute(
                'aria-selected',
                'true'
            );
            expect(styleEmptyState()).toBeTruthy();
            expect(quickEdit()).toBeNull();
        });

        it('should switch to the style tab when it is clicked', () => {
            spectator.detectChanges();

            spectator.click(byTestId('edit-panel-style-editor-tab'));
            spectator.detectChanges();

            expect(store.editorSetEditPanelTab).toHaveBeenCalledWith(
                UVE_EDIT_PANEL_TABS.STYLE_EDITOR
            );
            expect(styleEmptyState()).toBeTruthy();
            expect(quickEdit()).toBeNull();
        });

        it('should switch back to the quick-edit tab when it is clicked', () => {
            openStyleTab();

            spectator.click(byTestId('edit-panel-quick-edit-tab'));
            spectator.detectChanges();

            expect(store.editorSetEditPanelTab).toHaveBeenCalledWith(
                UVE_EDIT_PANEL_TABS.QUICK_EDIT
            );
            expect(quickEdit()).toBeTruthy();
            expect(styleEmptyState()).toBeNull();
        });
    });

    describe('quick-edit tab', () => {
        it('should give quick edit the selected contentlet from the store', () => {
            spectator.detectChanges();

            expect(quickEdit()?.data()).toEqual(store.$editorContentletEditData());
        });

        it("should show the contentlet's latest saved version after a save", () => {
            spectator.detectChanges();

            const saved = createEditData(
                createFakeContentlet({ contentType: 'Banner', inode: 'inode-after-save' })
            );
            store.$editorContentletEditData.set(saved);
            spectator.detectChanges();

            expect(quickEdit()?.data().contentlet.inode).toBe('inode-after-save');
        });

        it('should tell quick edit while the page is loading', () => {
            store.$editorIsIframeLoading.set(true);
            spectator.detectChanges();

            expect(quickEdit()?.loading()).toBe(true);
        });

        it('should show the no-permission state for a contentlet the user cannot edit', () => {
            store.$editorContentletEditData.set(
                createEditData(createFakeContentlet({ contentType: 'Banner', canEdit: false }))
            );
            store.editorCanEditSelectedContentlet.set(false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('quick-edit-no-permission'))).toBeTruthy();
        });
    });

    describe('style tab', () => {
        it('should show the style form when the content type has a style schema', () => {
            store.$styleSchema.set(STYLE_SCHEMA);
            openStyleTab();

            expect(styleForm()?.schema()).toEqual(STYLE_SCHEMA);
            expect(styleEmptyState()).toBeNull();
        });

        it('should show the empty state for the selected content type when it has no style schema', () => {
            openStyleTab();

            expect(styleEmptyState()?.contentTypeVar()).toBe('Banner');
            expect(styleForm()).toBeNull();
        });

        it('should give the empty state no content type when nothing is selected', () => {
            store.editorSelected.set(null);
            openStyleTab();

            expect(styleEmptyState()?.contentTypeVar()).toBe('');
        });

        it.each([
            { case: 'with a style schema', schema: STYLE_SCHEMA },
            { case: 'without a style schema', schema: undefined }
        ])(
            'should show the no-permission state $case for a contentlet the user cannot edit',
            ({ schema }) => {
                store.$styleSchema.set(schema);
                store.editorCanEditSelectedContentlet.set(false);
                openStyleTab();

                expect(spectator.query(byTestId('style-editor-no-permission'))).toHaveText(
                    MESSAGES['uve.contentlet.no.edit.permission']
                );
                expect(styleForm()).toBeNull();
                expect(styleEmptyState()).toBeNull();
            }
        );
    });
});
