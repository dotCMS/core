import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of, throwError } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotEditContentService,
    DotFileFieldComponent,
    DotTagFieldComponent
} from '@dotcms/edit-content';

import { DotUveQuickEditFormComponent } from './dot-uve-quick-edit-form.component';

import { UveOptimisticSaveService } from '../../../../services/uve-optimistic-save/uve-optimistic-save.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { UVEStore } from '../../../../store/dot-uve.store';
import { PageType } from '../../../../store/models';
import { ContentletEditData, ContentletField } from '../types';

const flushMicrotasks = () => new Promise<void>((resolve) => queueMicrotask(resolve));

const makeContentlet = (overrides: Partial<DotCMSContentlet> = {}): DotCMSContentlet =>
    ({
        identifier: 'id-1',
        inode: 'inode-1',
        contentType: 'Blog',
        title: 'Hello',
        ...overrides
    }) as DotCMSContentlet;

const makeData = (overrides: Partial<DotCMSContentlet> = {}): ContentletEditData => ({
    container: undefined,
    contentlet: makeContentlet(overrides)
});

const titleField: ContentletField = {
    name: 'Title',
    variable: 'title',
    clazz: DotCMSClazzes.TEXT,
    required: true,
    readOnly: false
} as ContentletField;

describe('DotUveQuickEditFormComponent', () => {
    let spectator: Spectator<DotUveQuickEditFormComponent>;
    let editorSelected: ReturnType<
        typeof signal<{ payload: { container: unknown; contentlet: DotCMSContentlet } } | null>
    >;
    let pageType: ReturnType<typeof signal<PageType>>;
    let saveQuickEditFields: jest.Mock;
    let setSelectedPayload: jest.Mock;
    let setUveStatus: jest.Mock;
    let pageReload: jest.Mock;
    let addCurrentPageToHistory: jest.Mock;
    let messageAdd: jest.Mock;
    let updateIframeOptimistically: jest.Mock;
    let extractFromRollback: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotUveQuickEditFormComponent,
        overrideComponents: [
            [
                DotUveQuickEditFormComponent,
                {
                    remove: {
                        imports: [DotFileFieldComponent, DotTagFieldComponent],
                        providers: [UveOptimisticSaveService, DotEditContentService]
                    },
                    add: {
                        imports: [
                            MockComponent(DotFileFieldComponent),
                            MockComponent(DotTagFieldComponent)
                        ]
                    }
                }
            ]
        ],
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    editorSelected,
                    pageType,
                    saveQuickEditFields,
                    setSelectedPayload,
                    setUveStatus,
                    pageReload,
                    addCurrentPageToHistory
                })
            },
            {
                provide: UveOptimisticSaveService,
                useFactory: () => ({
                    updateIframeOptimistically,
                    extractFromRollback
                })
            },
            {
                provide: DotEditContentService,
                useFactory: () => ({})
            },
            {
                provide: MessageService,
                useFactory: () => ({ add: messageAdd })
            },
            {
                provide: DotMessageService,
                useFactory: () => ({
                    get: jest.fn((key: string) => key)
                })
            }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false
    });

    beforeEach(() => {
        editorSelected = signal({
            payload: {
                container: { identifier: 'c1' },
                contentlet: makeContentlet()
            }
        });
        pageType = signal(PageType.HEADLESS);
        saveQuickEditFields = jest.fn().mockReturnValue(of(makeContentlet({ title: 'Saved' })));
        setSelectedPayload = jest.fn();
        setUveStatus = jest.fn();
        pageReload = jest.fn();
        addCurrentPageToHistory = jest.fn();
        messageAdd = jest.fn();
        updateIframeOptimistically = jest.fn();
        extractFromRollback = jest.fn().mockReturnValue({});
    });

    describe('form construction', () => {
        it('builds a form once data and fields resolve', async () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.component.$contentletForm()).not.toBeNull();
            expect(spectator.component.$contentletForm()?.get('title')?.value).toBe('Hello');
            expect(spectator.component.$contentletForm()?.get('inode')?.value).toBe('inode-1');
        });

        it('renders nothing when fields is empty', () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [] }
            });
            spectator.detectChanges();

            expect(spectator.component.$contentletForm()).toBeNull();
            expect(spectator.query('form')).toBeNull();
        });
    });

    describe('openFullEditor output', () => {
        it('emits when the open-full-editor button is clicked', async () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            const emitSpy = jest.fn();
            spectator.component.openFullEditor.subscribe(emitSpy);

            const button = spectator
                .query(byTestId('open-full-editor-button'))
                ?.querySelector('button');
            spectator.click(button as HTMLElement);

            expect(emitSpy).toHaveBeenCalled();
        });
    });

    describe('cancel', () => {
        it('emits closed', async () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            const emitSpy = jest.fn();
            spectator.component.closed.subscribe(emitSpy);

            const button = spectator.query(byTestId('cancel-button'))?.querySelector('button');
            spectator.click(button as HTMLElement);

            expect(emitSpy).toHaveBeenCalled();
        });
    });

    describe('save', () => {
        it('marks invalid form as touched and does not call saveQuickEditFields', async () => {
            spectator = createComponent({
                // contentlet has no title — required field fails validation.
                props: { data: makeData({ title: '' }), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            const button = spectator.query(byTestId('save-button'))?.querySelector('button');
            spectator.click(button as HTMLElement);

            const form = spectator.component.$contentletForm();
            expect(form?.get('title')?.touched).toBe(true);
            expect(saveQuickEditFields).not.toHaveBeenCalled();
        });

        it('does nothing when nothing has changed (not dirty)', async () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            const button = spectator.query(byTestId('save-button'))?.querySelector('button');
            spectator.click(button as HTMLElement);

            expect(saveQuickEditFields).not.toHaveBeenCalled();
        });

        it('saves dirty fields and patches the selection with the saved contentlet', async () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            spectator.component.$contentletForm()?.get('title')?.setValue('Updated');
            spectator.detectChanges();

            const button = spectator.query(byTestId('save-button'))?.querySelector('button');
            spectator.click(button as HTMLElement);

            expect(addCurrentPageToHistory).toHaveBeenCalled();
            expect(saveQuickEditFields).toHaveBeenCalledWith(
                expect.objectContaining({ title: 'Updated' })
            );
            expect(setSelectedPayload).toHaveBeenCalled();
            expect(messageAdd).toHaveBeenCalledWith(
                expect.objectContaining({ severity: 'success' })
            );
        });

        it('flips uveStatus to LOADING and triggers pageReload on TRADITIONAL pages', async () => {
            pageType.set(PageType.TRADITIONAL);

            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            spectator.component.$contentletForm()?.get('title')?.setValue('Updated');
            spectator.detectChanges();

            const button = spectator.query(byTestId('save-button'))?.querySelector('button');
            spectator.click(button as HTMLElement);

            expect(setUveStatus).toHaveBeenCalledWith(UVE_STATUS.LOADING);
            expect(pageReload).toHaveBeenCalled();
        });

        it('shows an error toast and reverts uveStatus to LOADED on save failure (TRADITIONAL)', async () => {
            pageType.set(PageType.TRADITIONAL);
            saveQuickEditFields.mockReturnValue(throwError(() => new Error('save failed')));

            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            spectator.component.$contentletForm()?.get('title')?.setValue('Updated');
            spectator.detectChanges();

            const button = spectator.query(byTestId('save-button'))?.querySelector('button');
            spectator.click(button as HTMLElement);

            expect(setUveStatus).toHaveBeenLastCalledWith(UVE_STATUS.LOADED);
            expect(messageAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }));
        });
    });

    describe('optimistic update on form change', () => {
        it('pushes value changes through UveOptimisticSaveService for HEADLESS pages', async () => {
            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            spectator.component.$contentletForm()?.get('title')?.setValue('Streamed');

            expect(updateIframeOptimistically).toHaveBeenCalled();
        });

        it('does not push optimistic updates on TRADITIONAL pages', async () => {
            pageType.set(PageType.TRADITIONAL);

            spectator = createComponent({
                props: { data: makeData(), fields: [titleField] }
            });
            spectator.detectChanges();
            await flushMicrotasks();
            spectator.detectChanges();

            spectator.component.$contentletForm()?.get('title')?.setValue('Streamed');

            expect(updateIframeOptimistically).not.toHaveBeenCalled();
        });
    });
});
