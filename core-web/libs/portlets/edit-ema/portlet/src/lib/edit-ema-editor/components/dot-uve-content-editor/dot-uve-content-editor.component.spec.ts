import { Dispatcher } from '@ngrx/signals/events';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { EMPTY, Observable, Subject, of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { Component, output, signal } from '@angular/core';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotContentTypeService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotContentletCanLock, DotTreeNode, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotEditContentDialogComponent, EditContentDialogData } from '@dotcms/edit-content';
import { DotCMSURLContentMap, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import {
    DOT_VELOCITY_LANGUAGE_ID,
    DotCopyContentModalService,
    DotSidePanelConfig,
    DotSidePanelService
} from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import {
    MockDotMessageService,
    createFakeContentType,
    createFakeContentlet
} from '@dotcms/utils-testing';

import { DotUveContentEditorComponent } from './dot-uve-content-editor.component';
import {
    DotUveAddContentRequest,
    DotUveCreateContentRequest,
    DotUveEditContentRequest,
    uveContentEditorEvents
} from './dot-uve-content-editor.events';

import { DotEmaDialogComponent } from '../../../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import {
    CONTAINER_INSERT_ERROR,
    FormStatus,
    NG_CUSTOM_EVENTS,
    UVE_STATUS
} from '../../../shared/enums';
import { EDIT_ACTION_PAYLOAD_MOCK } from '../../../shared/mocks';
import {
    ActionPayload,
    CreateFromPaletteAction,
    DialogAction,
    VTLFile
} from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { EditorInsertResult, ReloadEditorContent } from '../../../store/features/editor/models';
import { PageType } from '../../../store/models';

/**
 * Stands in for the legacy JSP dialog, which needs a running dotCMS. It exposes what the content
 * editor calls and the outputs it listens to, so a test can tell whether the dialog was asked to
 * open and can answer for it. Like the real dialog, it keeps the container position its create
 * form was opened for until it is told to forget it.
 */
@Component({
    selector: 'dot-edit-ema-dialog',
    template: ''
})
class DotEmaDialogStubComponent {
    readonly action = output<DialogAction>();
    readonly reloadFromDialog = output<void>();
    /** The container position the dialog sends back with every event, as the real one does. */
    actionPayload: ActionPayload | undefined;
    readonly editContentlet = vi.fn();
    readonly editVTLContentlet = vi.fn();
    readonly createContentletFromPalette = vi.fn(({ actionPayload }: CreateFromPaletteAction) => {
        this.actionPayload = actionPayload;
    });
    readonly createContentlet = vi.fn();
    readonly addContentlet = vi.fn();
    readonly addWidget = vi.fn();
    readonly addForm = vi.fn();
    readonly resetDialog = vi.fn();
    readonly resetActionPayload = vi.fn(() => {
        this.actionPayload = undefined;
    });
}

const CONTENTLET = createFakeContentlet({
    identifier: 'contentlet-identifier-123',
    inode: 'contentlet-inode-123',
    title: 'Hello World',
    contentType: 'test'
});

const MULTI_PAGE_CONTENTLET = createFakeContentlet({
    ...CONTENTLET,
    onNumberOfPages: 2
});

const COPIED_CONTENTLET = createFakeContentlet({
    identifier: 'copied-contentlet-identifier-456',
    inode: 'copied-contentlet-inode-456',
    title: 'Hello World',
    contentType: 'test'
});

/** The container the pencil was clicked in. */
const FORK_FROM = EDIT_ACTION_PAYLOAD_MOCK.container;

const TREE_NODE: DotTreeNode = {
    containerId: 'test',
    contentId: 'contentlet-identifier-123',
    pageId: 'test',
    relationType: 'test',
    treeOrder: '0'
};

const LEGACY_CONTENT_TYPE = createFakeContentType({
    variable: 'test',
    name: 'Test',
    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
});

const NEW_EDITOR_CONTENT_TYPE = createFakeContentType({
    variable: 'test',
    name: 'Test',
    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
});

const VTL_FILE: VTLFile = { inode: 'vtl-inode-123', name: 'my-template.vtl' };

const VELOCITY_FILE = createFakeContentlet({
    inode: 'vtl-inode-123',
    identifier: 'vtl-identifier-123',
    title: 'my-template.vtl',
    languageId: 2,
    baseType: 'FILEASSET',
    contentType: 'FileAsset',
    extension: 'vtl',
    fileName: 'my-template.vtl'
});

/** A file the source editor does not handle, so it goes to the content editor. */
const NON_SOURCE_FILE = createFakeContentlet({
    inode: 'vtl-inode-123',
    title: 'my-template.vtl',
    contentType: 'test'
});

const CAN_LOCK: DotContentletCanLock = {
    canLock: true,
    locked: false,
    id: '',
    inode: '',
    lockedBy: ''
};

/** The container position a palette drop or a container's "add" menu asks to insert into. */
const INTO: ActionPayload = EDIT_ACTION_PAYLOAD_MOCK;

/** The content type to create, as the SDK names it: by its variable only. */
const CONTENT_TYPE_TO_CREATE: DotUveCreateContentRequest['contentType'] = {
    variable: 'test',
    name: 'test'
};

/** A contentlet the new editor reports it created. */
const CREATED_CONTENTLET = createFakeContentlet({
    identifier: 'created-contentlet-identifier-789',
    inode: 'created-contentlet-inode-789',
    contentType: 'test'
});

/** What the store answers when the container is full. */
const CONTAINER_FULL: EditorInsertResult = {
    didInsert: false,
    errorCode: CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED,
    maxContentlets: 1
};

/** What the store answers when the container already holds the contentlet. */
const ALREADY_IN_CONTAINER: EditorInsertResult = {
    didInsert: false,
    errorCode: CONTAINER_INSERT_ERROR.DUPLICATE_CONTENT
};

/** The toast for a full container, naming how many contentlets it holds. */
const CONTAINER_FULL_TOAST = {
    severity: 'warn',
    summary: 'Container full',
    detail: 'This container holds up to 1 contentlets',
    life: 3000
};

/** The toast for a contentlet the container already holds. */
const ALREADY_IN_CONTAINER_TOAST = {
    severity: 'info',
    summary: 'Content already added',
    detail: 'This content is already added to this container',
    life: 2000
};

/** The options the editor has always opened the new editor's centered dialog with. */
const NEW_EDITOR_DIALOG_OPTIONS = {
    appendTo: 'body',
    baseZIndex: 10000,
    closable: true,
    closeOnEscape: true,
    draggable: false,
    keepInViewport: true,
    modal: true,
    resizable: true,
    position: 'center',
    width: '95%',
    height: '95%',
    maskStyleClass: 'p-dialog-mask-dynamic p-dialog-create-content',
    style: { 'max-width': '1400px', 'max-height': '900px' },
    contentStyle: { padding: '0' }
};

/**
 * The slice of `UVEStore` the content editor reads. Signals so a test can switch the mode or the flags and
 * see the template follow; methods as spies.
 */
function createStoreMock() {
    return {
        flags: signal<Record<string, boolean>>({}),
        editorCanEditContent: signal(true),
        viewMode: signal(UVE_MODE.EDIT),
        pageAsset: signal<{ urlContentMap?: DotCMSURLContentMap } | null>(null),
        pageParams: signal({ url: 'index' }),
        pageLanguageId: signal(1),
        $reloadEditorContent: signal<ReloadEditorContent>({
            code: undefined,
            pageType: PageType.TRADITIONAL,
            enableInlineEdit: false,
            pageAssetRef: {}
        }),
        getCurrentTreeNode: vi.fn().mockReturnValue(TREE_NODE),
        editorInsertContentlet: vi.fn<() => EditorInsertResult>(() => ({ didInsert: true })),
        setUveStatus: vi.fn(),
        pageReload: vi.fn(),
        pageLoad: vi.fn()
    };
}

describe('DotUveContentEditorComponent', () => {
    let spectator: Spectator<DotUveContentEditorComponent>;
    let store: ReturnType<typeof createStoreMock>;
    let contentTypeService: SpyObject<DotContentTypeService>;
    let contentletService: SpyObject<DotContentletService>;
    let copyContentService: SpyObject<DotCopyContentService>;
    let copyContentModalService: SpyObject<DotCopyContentModalService>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;
    let dialogService: SpyObject<DialogService>;
    let pageApiService: SpyObject<DotPageApiService>;
    let messageService: SpyObject<MessageService>;

    /** The side panels the content editor opened, recorded instead of rendered. */
    let opened: {
        config: DotSidePanelConfig;
        ref: { closed: Subject<void>; close: Mock; requestClose: Mock };
    }[];

    const createComponent = createComponentFactory({
        component: DotUveContentEditorComponent,
        overrideComponents: [
            [
                DotUveContentEditorComponent,
                {
                    remove: { imports: [DotEmaDialogComponent] },
                    add: { imports: [DotEmaDialogStubComponent] }
                }
            ]
        ],
        providers: [
            { provide: UVEStore, useFactory: () => store },
            mockProvider(DotContentTypeService),
            mockProvider(DotContentletService),
            mockProvider(DotCopyContentService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DialogService),
            mockProvider(DotSidePanelService),
            mockProvider(DotPageApiService),
            mockProvider(MessageService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'contenttypes.content.create.contenttype': 'Create {0}',
                    'editpage.content.add.already.title': 'Content already added',
                    'editpage.content.add.already.message':
                        'This content is already added to this container',
                    'editpage.content.container.limit.title': 'Container full',
                    'editpage.content.container.limit.message':
                        'This container holds up to {0} contentlets'
                })
            },
            { provide: WINDOW, useValue: window }
        ],
        componentProviders: [mockProvider(DotCopyContentModalService)]
    });

    /** Asks the content editor to edit a contentlet, as any part of the editor does. */
    const editContent = (request: DotUveEditContentRequest) => {
        spectator
            .inject(Dispatcher)
            .dispatch(uveContentEditorEvents.uveEditContentRequested(request));
        spectator.detectChanges();
    };

    /** Asks the content editor to edit a VTL file, as the contentlet tools' VTL menu does. */
    const editSource = (file: VTLFile) => {
        spectator.inject(Dispatcher).dispatch(uveContentEditorEvents.uveEditSourceRequested(file));
        spectator.detectChanges();
    };

    /**
     * Asks the content editor to create a contentlet, as a palette drop does with a container
     * position and the SDK's createContentlet() does without one.
     */
    const createContent = (request: DotUveCreateContentRequest) => {
        spectator
            .inject(Dispatcher)
            .dispatch(uveContentEditorEvents.uveCreateContentRequested(request));
        spectator.detectChanges();
    };

    /** Asks the content editor to add content to a container, as its "add" menu does. */
    const addContent = (request: DotUveAddContentRequest) => {
        spectator
            .inject(Dispatcher)
            .dispatch(uveContentEditorEvents.uveAddContentRequested(request));
        spectator.detectChanges();
    };

    const legacyDialog = () => spectator.query(DotEmaDialogStubComponent);

    /**
     * Sends an `ng-event` out of the legacy dialog, as the JSP inside it does. A dialog opened
     * for a container position sends that position back with every event (`actionPayload`).
     */
    const emitFromLegacyDialog = (
        name: NG_CUSTOM_EVENTS,
        payload?: unknown,
        { data, actionPayload }: { data?: unknown; actionPayload?: ActionPayload } = {}
    ) => {
        const action: DialogAction = {
            event: new CustomEvent('ng-event', { detail: { name, payload, data } }),
            actionPayload,
            form: { status: FormStatus.PRISTINE, isTranslation: false },
            clientAction: DotCMSUVEAction.NOOP
        };

        // Without a dialog there is nothing to emit from, and an "ignored" event would prove nothing.
        expect(legacyDialog()).toBeTruthy();
        spectator.triggerEventHandler(DotEmaDialogStubComponent, 'action', action);
    };

    /** The data the new editor was opened with, in its centered dialog. */
    const newEditorDialogData = () =>
        dialogService.open.mock.calls[0][1].data as EditContentDialogData;

    /** The data the new editor was opened with, in the side panel. */
    const sidePanelData = () =>
        (opened[0].config.inputs?.['data'] as () => EditContentDialogData)();

    beforeEach(() => {
        store = createStoreMock();
        opened = [];

        spectator = createComponent();

        contentTypeService = spectator.inject(DotContentTypeService);
        contentletService = spectator.inject(DotContentletService);
        copyContentService = spectator.inject(DotCopyContentService);
        copyContentModalService = spectator.inject(DotCopyContentModalService, true);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        dialogService = spectator.inject(DialogService);
        pageApiService = spectator.inject(DotPageApiService);
        messageService = spectator.inject(MessageService);

        contentTypeService.getContentType.mockReturnValue(of(LEGACY_CONTENT_TYPE));
        contentletService.getContentletByInode.mockReturnValue(of(VELOCITY_FILE));
        contentletService.canLock.mockReturnValue(of(CAN_LOCK));
        copyContentService.copyInPage.mockReturnValue(of(COPIED_CONTENTLET));
        copyContentModalService.open.mockReturnValue(of({ shouldCopy: false }));
        dialogService.open.mockReturnValue({ onClose: new Subject() } as never);
        spectator.inject(DotSidePanelService).open.mockImplementation((_component, config) => {
            const closed = new Subject<void>();
            const ref = {
                closed,
                close: vi.fn(() => {
                    closed.next();
                    closed.complete();
                }),
                requestClose: vi.fn()
            };
            opened.push({ config, ref });

            return ref as unknown as ReturnType<DotSidePanelService['open']>;
        });
    });

    describe('uveEditContentRequested', () => {
        // Every caller sends `{ contentlet }` (plus `forkFrom` from the pencil), the SDK's
        // editContentlet() included, so these tests are its path too.
        describe('choosing the editor', () => {
            it('should open the legacy dialog when the content type does not enable the new editor', () => {
                editContent({ contentlet: CONTENTLET });

                expect(contentTypeService.getContentType).toHaveBeenCalledWith('test');
                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(CONTENTLET);
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should open the new editor when CONTENT_EDITOR2_ENABLED is true on the content type', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

                editContent({ contentlet: CONTENTLET });

                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(newEditorDialogData()).toEqual(
                    expect.objectContaining({
                        mode: 'edit',
                        contentletInode: 'contentlet-inode-123'
                    })
                );
            });

            it('should fall back to the legacy dialog when the content type lookup fails', () => {
                contentTypeService.getContentType.mockReturnValue(
                    throwError(() => new Error('network error'))
                );

                editContent({ contentlet: CONTENTLET });

                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(CONTENTLET);
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should open the legacy dialog without a lookup when the contentlet has no content type', () => {
                const contentlet = createFakeContentlet({ ...CONTENTLET, contentType: undefined });

                editContent({ contentlet });

                expect(contentTypeService.getContentType).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(contentlet);
            });

            it('should not check edit permission itself', () => {
                // The pencil and quick edit refuse a contentlet the user cannot edit before they
                // ask; page Properties never checked. The content editor opens what it is asked
                // to open.
                const contentlet = createFakeContentlet({ ...CONTENTLET, canEdit: false });

                editContent({ contentlet });

                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(contentlet);
            });
        });

        describe('new editor', () => {
            beforeEach(() => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
            });

            it("should open in a centered dialog with the editor's options and the contentlet title as header", () => {
                editContent({ contentlet: CONTENTLET });

                expect(dialogService.open).toHaveBeenCalledWith(DotEditContentDialogComponent, {
                    ...NEW_EDITOR_DIALOG_OPTIONS,
                    data: {
                        mode: 'edit',
                        contentletInode: 'contentlet-inode-123',
                        onContentSaved: expect.any(Function)
                    },
                    header: 'Hello World'
                });
                expect(opened).toHaveLength(0);
            });

            it('should use an empty header when the contentlet has no title', () => {
                editContent({
                    contentlet: createFakeContentlet({ ...CONTENTLET, title: undefined })
                });

                expect(dialogService.open.mock.calls[0][1].header).toBe('');
            });

            it('should reload the page when the new editor saves', () => {
                editContent({ contentlet: CONTENTLET });

                newEditorDialogData().onContentSaved?.(CONTENTLET);

                expect(store.pageReload).toHaveBeenCalled();
            });

            describe('in the side panel', () => {
                beforeEach(() => {
                    store.flags.set({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: true });
                });

                it('should open in the side panel instead of the dialog when its flag is on', () => {
                    editContent({ contentlet: CONTENTLET });

                    expect(dialogService.open).not.toHaveBeenCalled();
                    expect(opened).toHaveLength(1);
                    const [{ config }] = opened;
                    expect((config.heading as () => string)()).toBe('Hello World');
                    expect((config.inputs?.['data'] as () => unknown)()).toEqual(
                        expect.objectContaining({
                            mode: 'edit',
                            contentletInode: 'contentlet-inode-123',
                            title: 'Hello World'
                        })
                    );
                });

                it('should reload the page when the side panel editor saves', () => {
                    editContent({ contentlet: CONTENTLET });

                    const data = (
                        opened[0].config.inputs?.['data'] as () => {
                            onContentSaved: () => void;
                        }
                    )();
                    data.onContentSaved();

                    expect(store.pageReload).toHaveBeenCalled();
                });

                it('should open the side panel again after it closed itself', () => {
                    editContent({ contentlet: CONTENTLET });

                    opened[0].ref.close();
                    editContent({ contentlet: CONTENTLET });

                    // The close cleared the request, so the same contentlet opens a new panel.
                    expect(opened).toHaveLength(2);
                });
            });
        });

        describe('copy prompt for a contentlet on more than one page', () => {
            it('should ask first, and open nothing until the user answers', () => {
                copyContentModalService.open.mockReturnValue(
                    new Subject<{ shouldCopy: boolean }>()
                );

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(copyContentModalService.open).toHaveBeenCalled();
                expect(contentTypeService.getContentType).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should ask when the number of pages comes from the page markup as text', () => {
                copyContentModalService.open.mockReturnValue(
                    new Subject<{ shouldCopy: boolean }>()
                );

                editContent({
                    contentlet: createFakeContentlet({ ...CONTENTLET, onNumberOfPages: '2' }),
                    forkFrom: FORK_FROM
                });

                expect(copyContentModalService.open).toHaveBeenCalled();
            });

            it('should not ask for a contentlet on a single page', () => {
                editContent({
                    contentlet: createFakeContentlet({ ...CONTENTLET, onNumberOfPages: 1 }),
                    forkFrom: FORK_FROM
                });

                expect(copyContentModalService.open).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).toHaveBeenCalled();
            });

            it('should not ask when the request does not say which container to fork from', () => {
                // Quick edit's full editor and page Properties have always opened directly.
                editContent({ contentlet: MULTI_PAGE_CONTENTLET });

                expect(copyContentModalService.open).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(MULTI_PAGE_CONTENTLET);
            });

            it('should open the legacy dialog on the original when the user edits all pages', () => {
                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(copyContentService.copyInPage).not.toHaveBeenCalled();
                expect(store.pageReload).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(MULTI_PAGE_CONTENTLET);
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should open the new editor on the original when the user edits all pages', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(newEditorDialogData()).toEqual(
                    expect.objectContaining({
                        mode: 'edit',
                        contentletInode: 'contentlet-inode-123'
                    })
                );
            });

            it('should copy from the container it was picked in when the user edits this page only', () => {
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(store.getCurrentTreeNode).toHaveBeenCalledWith(
                    FORK_FROM,
                    MULTI_PAGE_CONTENTLET
                );
                expect(copyContentService.copyInPage).toHaveBeenCalledWith(TREE_NODE);
            });

            it('should reload the page and open the new editor on the copy when the user edits this page only', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(store.pageReload).toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(newEditorDialogData()).toEqual(
                    expect.objectContaining({
                        mode: 'edit',
                        contentletInode: 'copied-contentlet-inode-456'
                    })
                );
            });

            it('should fall back to the legacy dialog on the copy, and still reload, when the content type lookup fails', () => {
                contentTypeService.getContentType.mockReturnValue(
                    throwError(() => new Error('network error'))
                );
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(store.pageReload).toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(COPIED_CONTENTLET);
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should open nothing when the user dismisses the prompt', () => {
                copyContentModalService.open.mockReturnValue(EMPTY);

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(copyContentService.copyInPage).not.toHaveBeenCalled();
                expect(contentTypeService.getContentType).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should report a failed copy and open nothing', () => {
                const error = new Error('copy failed');
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));
                copyContentService.copyInPage.mockReturnValue(throwError(() => error));

                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
                expect(store.pageReload).not.toHaveBeenCalled();
                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(dialogService.open).not.toHaveBeenCalled();
            });
        });

        describe('each request on its own', () => {
            it('should keep handling requests after one failed', () => {
                copyContentModalService.open.mockReturnValue(of({ shouldCopy: true }));
                copyContentService.copyInPage.mockReturnValue(
                    throwError(() => new Error('copy failed'))
                );
                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                editContent({ contentlet: CONTENTLET });

                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(CONTENTLET);
            });

            it('should not cancel a request still waiting on the copy prompt when another one arrives', () => {
                const answer = new Subject<{ shouldCopy: boolean }>();
                copyContentModalService.open.mockReturnValue(answer);
                editContent({ contentlet: MULTI_PAGE_CONTENTLET, forkFrom: FORK_FROM });

                editContent({ contentlet: CONTENTLET });
                answer.next({ shouldCopy: false });
                answer.complete();

                expect(legacyDialog()?.editContentlet).toHaveBeenNthCalledWith(1, CONTENTLET);
                expect(legacyDialog()?.editContentlet).toHaveBeenNthCalledWith(
                    2,
                    MULTI_PAGE_CONTENTLET
                );
            });
        });
    });

    describe('uveEditSourceRequested', () => {
        it('should open the legacy VTL dialog when the file lookup fails', () => {
            contentletService.getContentletByInode.mockReturnValue(
                throwError(() => new Error('network error'))
            );

            editSource(VTL_FILE);

            expect(legacyDialog()?.editVTLContentlet).toHaveBeenCalledWith(VTL_FILE);
            expect(opened).toHaveLength(0);
        });

        it('should open the legacy dialog when the file is not source-editable and its content type does not enable the new editor', () => {
            contentletService.getContentletByInode.mockReturnValue(of(NON_SOURCE_FILE));

            editSource(VTL_FILE);

            expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(NON_SOURCE_FILE);
            expect(dialogService.open).not.toHaveBeenCalled();
        });

        it('should open the new editor when the file is not source-editable and its content type enables it', () => {
            contentletService.getContentletByInode.mockReturnValue(of(NON_SOURCE_FILE));
            contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

            editSource(VTL_FILE);

            expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
            expect(newEditorDialogData()).toEqual(
                expect.objectContaining({ mode: 'edit', contentletInode: 'vtl-inode-123' })
            );
        });

        it('should not ask to copy a file that is on more than one page', () => {
            contentletService.getContentletByInode.mockReturnValue(
                of(createFakeContentlet({ ...NON_SOURCE_FILE, onNumberOfPages: 2 }))
            );

            editSource(VTL_FILE);

            expect(copyContentModalService.open).not.toHaveBeenCalled();
            expect(legacyDialog()?.editContentlet).toHaveBeenCalled();
        });

        describe('Edit Code panel', () => {
            it("should open a Velocity file's source instead of the content editor", () => {
                editSource(VTL_FILE);

                const [{ config }] = opened;
                expect((config.inputs?.['request'] as () => unknown)()).toEqual({
                    inode: 'vtl-inode-123',
                    title: 'my-template.vtl',
                    language: DOT_VELOCITY_LANGUAGE_ID
                });
                expect((config.heading as () => string)()).toBe('my-template.vtl');
                expect(legacyDialog()?.editContentlet).not.toHaveBeenCalled();
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it.each([
                [
                    'the user cannot edit it',
                    of<DotContentletCanLock>({
                        canLock: false,
                        locked: true,
                        id: '',
                        inode: '',
                        lockedBy: 'someone'
                    })
                ],
                ['the lock check fails', throwError(() => new Error('network'))]
            ])('should open the content editor instead when %s', (_label, lockAnswer) => {
                contentletService.canLock.mockReturnValue(
                    lockAnswer as Observable<DotContentletCanLock>
                );

                editSource(VTL_FILE);

                expect(opened).toHaveLength(0);
                expect(contentTypeService.getContentType).toHaveBeenCalledWith('FileAsset');
                expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(VELOCITY_FILE);
            });

            it('should reload the page when the source is saved', () => {
                editSource(VTL_FILE);

                (opened[0].config.outputs?.['saved'] as () => void)();

                expect(store.pageReload).toHaveBeenCalled();
            });

            it('should open the file again after the panel closed itself', () => {
                editSource(VTL_FILE);

                opened[0].ref.close();
                editSource(VTL_FILE);

                // The close cleared the request, so the same file opens a new panel.
                expect(opened).toHaveLength(2);
            });
        });
    });

    describe('uveCreateContentRequested', () => {
        describe('choosing the editor', () => {
            it("should open the legacy create form, in the page's language, when the content type does not enable the new editor", () => {
                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                expect(contentTypeService.getContentType).toHaveBeenCalledWith('test');
                expect(legacyDialog()?.createContentletFromPalette).toHaveBeenCalledWith({
                    variable: 'test',
                    name: 'test',
                    actionPayload: INTO,
                    language_id: 1
                });
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should fall back to the legacy create form when the content type lookup fails', () => {
                contentTypeService.getContentType.mockReturnValue(
                    throwError(() => new Error('network error'))
                );

                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                expect(legacyDialog()?.createContentletFromPalette).toHaveBeenCalledWith(
                    expect.objectContaining({ variable: 'test', actionPayload: INTO })
                );
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should open the new editor, titled with the content type name, when CONTENT_EDITOR2_ENABLED is true on the content type', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                expect(legacyDialog()?.createContentletFromPalette).not.toHaveBeenCalled();
                expect(dialogService.open).toHaveBeenCalledWith(DotEditContentDialogComponent, {
                    ...NEW_EDITOR_DIALOG_OPTIONS,
                    data: {
                        mode: 'new',
                        contentTypeId: 'test',
                        onContentSaved: expect.any(Function)
                    },
                    header: 'Create Test'
                });
            });

            it("should title the new editor with the request's name when the content type has none", () => {
                contentTypeService.getContentType.mockReturnValue(
                    of(createFakeContentType({ ...NEW_EDITOR_CONTENT_TYPE, name: undefined }))
                );

                createContent({ contentType: { variable: 'test', name: 'Blog' }, into: INTO });

                expect(dialogService.open.mock.calls[0][1].header).toBe('Create Blog');
            });

            it('should close the legacy dialog before opening the new editor', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
            });

            it('should open the new editor in the side panel instead of the dialog when its flag is on', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
                store.flags.set({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: true });

                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                expect(dialogService.open).not.toHaveBeenCalled();
                expect(opened).toHaveLength(1);
                expect((opened[0].config.heading as () => string)()).toBe('Create Test');
                expect(sidePanelData()).toEqual(
                    expect.objectContaining({
                        mode: 'new',
                        contentTypeId: 'test',
                        title: 'Create Test'
                    })
                );
            });
        });

        describe('with a container position', () => {
            beforeEach(() => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
            });

            it('should insert what the new editor saves into the requested position', () => {
                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                newEditorDialogData().onContentSaved?.(CREATED_CONTENTLET);

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    INTO,
                    'created-contentlet-identifier-789'
                );
                expect(store.pageReload).not.toHaveBeenCalled();
                expect(messageService.add).not.toHaveBeenCalled();
            });

            it('should tell the user why when the container refuses what the new editor saved', () => {
                store.editorInsertContentlet.mockReturnValue(CONTAINER_FULL);
                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                newEditorDialogData().onContentSaved?.(CREATED_CONTENTLET);

                expect(messageService.add).toHaveBeenCalledWith(CONTAINER_FULL_TOAST);
            });

            it('should insert what the side panel editor saves into the requested position', () => {
                store.flags.set({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: true });
                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                sidePanelData().onContentSaved?.(CREATED_CONTENTLET);

                expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                    INTO,
                    'created-contentlet-identifier-789'
                );
            });

            it('should insert nothing when the new editor saves without an identifier', () => {
                createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                newEditorDialogData().onContentSaved?.(
                    createFakeContentlet({ ...CREATED_CONTENTLET, identifier: undefined })
                );

                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
            });
        });

        // What the SDK's createContentlet() asks for: the contentlet is only created, so nothing
        // goes into a container and the page reloads once it is saved.
        describe('without a container position', () => {
            it('should follow the content type and open the new editor when it enables it', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

                createContent({ contentType: CONTENT_TYPE_TO_CREATE });

                expect(legacyDialog()?.createContentletFromPalette).not.toHaveBeenCalled();
                expect(newEditorDialogData()).toEqual(
                    expect.objectContaining({ mode: 'new', contentTypeId: 'test' })
                );
            });

            it('should reload the page instead of inserting when the new editor saves', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
                createContent({ contentType: CONTENT_TYPE_TO_CREATE });

                newEditorDialogData().onContentSaved?.(CREATED_CONTENTLET);

                expect(store.pageReload).toHaveBeenCalled();
                expect(store.editorInsertContentlet).not.toHaveBeenCalled();
            });

            it('should open the legacy create form with no position when the content type does not enable the new editor', () => {
                createContent({ contentType: CONTENT_TYPE_TO_CREATE });

                expect(legacyDialog()?.createContentletFromPalette).toHaveBeenCalledWith({
                    variable: 'test',
                    name: 'test',
                    actionPayload: undefined,
                    language_id: 1
                });
            });
        });
    });

    describe('uveAddContentRequested', () => {
        it.each([
            ['content', 'addContentlet'],
            ['widget', 'addWidget'],
            ['form', 'addForm']
        ] as const)(
            'should open the legacy dialog to pick %s for the requested position',
            (type, method) => {
                addContent({ type, into: INTO });

                expect(legacyDialog()?.[method]).toHaveBeenCalledWith(INTO);
            }
        );
    });

    describe('legacy dialog', () => {
        describe('availability', () => {
            // The legacy dialog is always there, so a legacy edit opens in any mode and on any
            // page, as page Properties always has on the layout, rules and experiments routes.
            it('should render the legacy dialog in edit mode', () => {
                expect(spectator.query(byTestId('uve-content-editor-dialog'))).toBeTruthy();
            });

            it('should render the legacy dialog in preview mode', () => {
                store.viewMode.set(UVE_MODE.PREVIEW);
                spectator.detectChanges();

                expect(spectator.query(byTestId('uve-content-editor-dialog'))).toBeTruthy();
            });

            it('should render the legacy dialog when the user cannot edit the page', () => {
                store.editorCanEditContent.set(false);
                spectator.detectChanges();

                expect(spectator.query(byTestId('uve-content-editor-dialog'))).toBeTruthy();
            });

            describe.each([
                ['in preview mode', () => store.viewMode.set(UVE_MODE.PREVIEW)],
                ['when the user cannot edit the page', () => store.editorCanEditContent.set(false)]
            ])('%s', (_state, enterState) => {
                beforeEach(() => {
                    enterState();
                    spectator.detectChanges();
                });

                it('should open the legacy dialog for a content type not on the new editor', () => {
                    editContent({ contentlet: CONTENTLET });

                    expect(legacyDialog()?.editContentlet).toHaveBeenCalledWith(CONTENTLET);
                });

                it('should open the legacy VTL dialog when a file lookup fails', () => {
                    contentletService.getContentletByInode.mockReturnValue(
                        throwError(() => new Error('network error'))
                    );

                    editSource(VTL_FILE);

                    expect(legacyDialog()?.editVTLContentlet).toHaveBeenCalledWith(VTL_FILE);
                });
            });

            it('should still open the new editor in preview mode', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
                store.viewMode.set(UVE_MODE.PREVIEW);
                spectator.detectChanges();

                editContent({ contentlet: CONTENTLET });

                expect(dialogService.open).toHaveBeenCalled();
            });

            it('should still open the new editor when the user cannot edit the page', () => {
                contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
                store.editorCanEditContent.set(false);
                spectator.detectChanges();

                editContent({ contentlet: CONTENTLET });

                expect(dialogService.open).toHaveBeenCalled();
            });
        });

        describe('events', () => {
            it('should reload the page when the legacy editor saves', () => {
                emitFromLegacyDialog(NG_CUSTOM_EVENTS.SAVE_PAGE, {
                    contentletIdentifier: 'contentlet-identifier-123'
                });

                expect(store.pageReload).toHaveBeenCalled();
            });

            it('should reload rather than follow a new page URL when the legacy editor saves', () => {
                emitFromLegacyDialog(NG_CUSTOM_EVENTS.SAVE_PAGE, {
                    htmlPageReferer: '/renamed-page?com.dotmarketing.htmlpage.language=1'
                });

                expect(store.pageReload).toHaveBeenCalled();
                expect(store.pageLoad).not.toHaveBeenCalled();
            });

            it('should load the new URL in the new language when a translation moved the page', () => {
                emitFromLegacyDialog(NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED, {
                    htmlPageReferer: '/new-url-here?com.dotmarketing.htmlpage.language=1'
                });

                expect(store.pageLoad).toHaveBeenCalledWith({
                    url: '/new-url-here',
                    language_id: '1'
                });
            });

            it('should load the new language when a translation kept the URL', () => {
                store.pageParams.set({ url: 'test-url' });

                emitFromLegacyDialog(NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED, {
                    htmlPageReferer: '/test-url?com.dotmarketing.htmlpage.language=2'
                });

                expect(store.pageLoad).toHaveBeenCalledWith({ language_id: '2' });
            });

            it('should reload the page when the legacy dialog asks to', () => {
                spectator.triggerEventHandler(
                    DotEmaDialogStubComponent,
                    'reloadFromDialog',
                    undefined
                );

                expect(store.pageReload).toHaveBeenCalled();
            });

            it.each([
                NG_CUSTOM_EVENTS.DIALOG_CLOSED,
                NG_CUSTOM_EVENTS.EDIT_CONTENTLET_LOADED,
                NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED,
                NG_CUSTOM_EVENTS.COMPARE_CONTENTLET,
                NG_CUSTOM_EVENTS.OPEN_WIZARD,
                NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION
            ])('should ignore %s', (name) => {
                emitFromLegacyDialog(name);

                expect(store.pageReload).not.toHaveBeenCalled();
                expect(store.pageLoad).not.toHaveBeenCalled();
            });
        });

        // A dialog opened by a container's "add" menu or a palette drop sends back the container
        // position it was opened for with every event, and what the user picks or creates in it
        // goes into that position.
        describe('content picked or created for a container position', () => {
            describe('CONTENT_SEARCH_SELECT', () => {
                const selectContent = () =>
                    emitFromLegacyDialog(NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT, undefined, {
                        data: { identifier: 'picked-contentlet-identifier', inode: 'picked-inode' },
                        actionPayload: INTO
                    });

                it('should insert the picked contentlet into the position and close the dialog', () => {
                    selectContent();

                    expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                        INTO,
                        'picked-contentlet-identifier'
                    );
                    expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
                });

                it('should tell the user why and close the dialog when the container refuses the contentlet', () => {
                    store.editorInsertContentlet.mockReturnValue(ALREADY_IN_CONTAINER);

                    selectContent();

                    expect(messageService.add).toHaveBeenCalledWith(ALREADY_IN_CONTAINER_TOAST);
                    expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
                });
            });

            describe('SAVE_PAGE', () => {
                const saveInDialog = () =>
                    emitFromLegacyDialog(
                        NG_CUSTOM_EVENTS.SAVE_PAGE,
                        { contentletIdentifier: 'saved-contentlet-identifier' },
                        { actionPayload: INTO }
                    );

                it('should insert the saved contentlet into the position instead of reloading the page', () => {
                    saveInDialog();

                    expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                        INTO,
                        'saved-contentlet-identifier'
                    );
                    expect(store.pageReload).not.toHaveBeenCalled();
                });

                it('should leave the dialog open when the contentlet went in', () => {
                    saveInDialog();

                    expect(legacyDialog()?.resetDialog).not.toHaveBeenCalled();
                });

                it('should tell the user why and close the dialog when the container refuses the contentlet', () => {
                    store.editorInsertContentlet.mockReturnValue(CONTAINER_FULL);

                    saveInDialog();

                    expect(messageService.add).toHaveBeenCalledWith(CONTAINER_FULL_TOAST);
                    expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
                });
            });

            describe('FORM_SELECTED', () => {
                const selectForm = () =>
                    emitFromLegacyDialog(NG_CUSTOM_EVENTS.FORM_SELECTED, undefined, {
                        data: { identifier: 'form-identifier' },
                        actionPayload: INTO
                    });

                beforeEach(() => {
                    pageApiService.getFormIndetifier.mockReturnValue(
                        of('form-in-container-identifier')
                    );
                });

                it('should insert the form under the identifier its container gives it, showing the page as loading meanwhile', () => {
                    selectForm();

                    expect(pageApiService.getFormIndetifier).toHaveBeenCalledWith(
                        INTO.container.identifier,
                        'form-identifier'
                    );
                    expect(store.setUveStatus).toHaveBeenCalledWith(UVE_STATUS.LOADING);
                    expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                        INTO,
                        'form-in-container-identifier'
                    );
                    expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
                });

                it('should tell the user why, stop loading and close the dialog when the container refuses the form', () => {
                    store.editorInsertContentlet.mockReturnValue(ALREADY_IN_CONTAINER);

                    selectForm();

                    expect(messageService.add).toHaveBeenCalledWith(ALREADY_IN_CONTAINER_TOAST);
                    expect(store.setUveStatus).toHaveBeenLastCalledWith(UVE_STATUS.LOADED);
                    expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
                });

                it('should insert nothing when the container cannot resolve the form', () => {
                    pageApiService.getFormIndetifier.mockReturnValue(
                        throwError(() => new Error('network error'))
                    );

                    selectForm();

                    expect(store.editorInsertContentlet).not.toHaveBeenCalled();
                    expect(store.setUveStatus).not.toHaveBeenCalled();
                });
            });

            // The content search's "create new" button: the JSP names the content type and the
            // legacy create form's URL.
            describe('CREATE_CONTENTLET', () => {
                const createFromSearch = () =>
                    emitFromLegacyDialog(NG_CUSTOM_EVENTS.CREATE_CONTENTLET, undefined, {
                        data: { contentType: 'test', url: 'test/url' },
                        actionPayload: INTO
                    });

                it('should close the legacy dialog and open the new editor, titled with the content type name, when the content type enables it', () => {
                    contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));

                    createFromSearch();

                    expect(contentTypeService.getContentType).toHaveBeenCalledWith('test');
                    expect(legacyDialog()?.resetDialog).toHaveBeenCalled();
                    expect(legacyDialog()?.createContentlet).not.toHaveBeenCalled();
                    expect(dialogService.open).toHaveBeenCalledWith(
                        DotEditContentDialogComponent,
                        expect.objectContaining({
                            data: expect.objectContaining({ mode: 'new', contentTypeId: 'test' }),
                            header: 'Create Test'
                        })
                    );
                });

                it('should insert what the new editor saves into the position the dialog was opened for', () => {
                    contentTypeService.getContentType.mockReturnValue(of(NEW_EDITOR_CONTENT_TYPE));
                    createFromSearch();

                    newEditorDialogData().onContentSaved?.(CREATED_CONTENTLET);

                    expect(store.editorInsertContentlet).toHaveBeenCalledWith(
                        INTO,
                        'created-contentlet-identifier-789'
                    );
                });

                it("should open the legacy create form at the dialog's URL when the content type does not enable the new editor", () => {
                    createFromSearch();

                    expect(legacyDialog()?.createContentlet).toHaveBeenCalledWith({
                        contentType: 'test',
                        url: 'test/url',
                        actionPayload: INTO
                    });
                    expect(dialogService.open).not.toHaveBeenCalled();
                });

                it("should fall back to the legacy create form at the dialog's URL when the content type lookup fails", () => {
                    contentTypeService.getContentType.mockReturnValue(
                        throwError(() => new Error('network error'))
                    );

                    createFromSearch();

                    expect(legacyDialog()?.createContentlet).toHaveBeenCalledWith({
                        contentType: 'test',
                        url: 'test/url',
                        actionPayload: INTO
                    });
                    expect(dialogService.open).not.toHaveBeenCalled();
                });
            });

            // A save into a container reloads the page content, and the legacy create form stays
            // open after its first save.
            describe('after the page content reloads', () => {
                /** What the store reports each time the page content reloads. */
                const reloadPageContent = () => {
                    store.$reloadEditorContent.set({
                        ...store.$reloadEditorContent(),
                        pageAssetRef: {}
                    });
                    spectator.detectChanges();
                };

                /** Saves in the legacy create form, which sends back the position it holds. */
                const saveInCreateForm = () =>
                    emitFromLegacyDialog(
                        NG_CUSTOM_EVENTS.SAVE_PAGE,
                        { contentletIdentifier: 'saved-contentlet-identifier' },
                        { actionPayload: legacyDialog()?.actionPayload }
                    );

                it("should clear the legacy dialog's container position", () => {
                    legacyDialog()?.resetActionPayload.mockClear();

                    reloadPageContent();

                    expect(legacyDialog()?.resetActionPayload).toHaveBeenCalledTimes(1);
                });

                it('should only reload, without inserting again, when the create form saves a second time', () => {
                    createContent({ contentType: CONTENT_TYPE_TO_CREATE, into: INTO });

                    saveInCreateForm();
                    reloadPageContent();
                    saveInCreateForm();

                    expect(store.editorInsertContentlet).toHaveBeenCalledTimes(1);
                    expect(store.pageReload).toHaveBeenCalledTimes(1);
                });
            });
        });
    });
});
