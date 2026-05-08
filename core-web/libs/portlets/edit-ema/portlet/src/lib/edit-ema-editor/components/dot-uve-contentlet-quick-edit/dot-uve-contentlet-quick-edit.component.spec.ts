import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { signal } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotUveContentletQuickEditComponent } from './dot-uve-contentlet-quick-edit.component';
import { DotUveCopyDecisionComponent } from './dot-uve-copy-decision/dot-uve-copy-decision.component';
import { DotUveQuickEditFormComponent } from './dot-uve-quick-edit-form/dot-uve-quick-edit-form.component';
import { ContentletEditData, TEMP_EMPTY_CONTENTLET_TYPE } from './types';

import { UVEStore } from '../../../store/dot-uve.store';

const makeContentlet = (overrides: Partial<DotCMSContentlet> = {}): DotCMSContentlet =>
    ({
        identifier: 'id-1',
        inode: 'inode-1',
        contentType: 'Blog',
        ...overrides
    }) as DotCMSContentlet;

const makeData = (overrides: Partial<DotCMSContentlet> = {}): ContentletEditData => ({
    container: undefined,
    contentlet: makeContentlet(overrides)
});

describe('DotUveContentletQuickEditComponent', () => {
    let spectator: Spectator<DotUveContentletQuickEditComponent>;
    let contentTypeCache: ReturnType<typeof signal<Record<string, unknown>>>;
    let loadContentType: jest.Mock;
    let resetSelected: jest.Mock;
    let setEditPanelOpen: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotUveContentletQuickEditComponent,
        overrideComponents: [
            [
                DotUveContentletQuickEditComponent,
                {
                    remove: {
                        imports: [DotUveCopyDecisionComponent, DotUveQuickEditFormComponent]
                    },
                    add: {
                        imports: [
                            MockComponent(DotUveCopyDecisionComponent),
                            MockComponent(DotUveQuickEditFormComponent)
                        ]
                    }
                }
            ]
        ],
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    contentTypeCache,
                    loadContentType,
                    resetSelected,
                    setEditPanelOpen
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        contentTypeCache = signal<Record<string, unknown>>({});
        loadContentType = jest.fn();
        resetSelected = jest.fn();
        setEditPanelOpen = jest.fn();
    });

    describe('$mode', () => {
        it('returns "empty" for the TEMP_EMPTY_CONTENTLET_TYPE sentinel', () => {
            spectator = createComponent({
                props: {
                    data: makeData({ contentType: TEMP_EMPTY_CONTENTLET_TYPE })
                }
            });
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('empty');
            expect(spectator.query(byTestId('empty-container'))).toBeTruthy();
        });

        it('returns "decide" when the contentlet appears on >1 pages and no decision yet', () => {
            spectator = createComponent({
                props: {
                    data: makeData({ onNumberOfPages: 3 })
                }
            });
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('decide');
        });

        it('returns "loading" when content type cache has no entry yet', () => {
            spectator = createComponent({
                props: {
                    data: makeData({ contentType: 'Blog', onNumberOfPages: 1 })
                }
            });
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('loading');
            expect(spectator.query(byTestId('loading-content-type'))).toBeTruthy();
        });

        it('returns "no-fields" when the layout has no quick-edit fields', () => {
            contentTypeCache.set({ Blog: { layout: [] } });
            spectator = createComponent({
                props: {
                    data: makeData({ contentType: 'Blog', onNumberOfPages: 1 })
                }
            });
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('no-fields');
            expect(spectator.query(byTestId('empty-no-fields'))).toBeTruthy();
        });

        it('returns "no-selection" when there is no contentType nor identifier', () => {
            spectator = createComponent({
                props: {
                    data: { container: undefined, contentlet: {} as DotCMSContentlet }
                }
            });
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('no-selection');
            expect(spectator.query(byTestId('empty-no-selection'))).toBeTruthy();
        });
    });

    describe('decision flag', () => {
        it('switches from "decide" to "loading" once handleDecisionMade is called', () => {
            spectator = createComponent({
                props: {
                    data: makeData({ contentType: 'Blog', onNumberOfPages: 5 })
                }
            });
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('decide');

            // protected method — invoke through any-cast to flip the flag
            (spectator.component as unknown as { handleDecisionMade(): void }).handleDecisionMade();
            spectator.detectChanges();

            expect(spectator.component.$mode()).toBe('loading');
        });

        it('resets the decision flag when the contentlet identifier changes', () => {
            spectator = createComponent({
                props: {
                    data: makeData({ identifier: 'id-1', onNumberOfPages: 3 })
                }
            });
            spectator.detectChanges();

            (spectator.component as unknown as { handleDecisionMade(): void }).handleDecisionMade();
            spectator.detectChanges();
            expect(spectator.component.$mode()).not.toBe('decide');

            spectator.setInput('data', makeData({ identifier: 'id-2', onNumberOfPages: 3 }));
            spectator.detectChanges();
            expect(spectator.component.$mode()).toBe('decide');
        });
    });

    describe('content-type loading effect', () => {
        it('calls loadContentType when contentType is set and not the sentinel', () => {
            spectator = createComponent({
                props: { data: makeData({ contentType: 'Blog' }) }
            });
            spectator.detectChanges();

            expect(loadContentType).toHaveBeenCalledWith('Blog');
        });

        it('does not call loadContentType for the empty-container sentinel', () => {
            spectator = createComponent({
                props: { data: makeData({ contentType: TEMP_EMPTY_CONTENTLET_TYPE }) }
            });
            spectator.detectChanges();

            expect(loadContentType).not.toHaveBeenCalled();
        });
    });

    describe('closePanel', () => {
        it('clears selection and closes the edit panel', () => {
            spectator = createComponent({
                props: { data: makeData({ contentType: TEMP_EMPTY_CONTENTLET_TYPE }) }
            });
            spectator.detectChanges();

            (spectator.component as unknown as { closePanel(): void }).closePanel();

            expect(resetSelected).toHaveBeenCalled();
            expect(setEditPanelOpen).toHaveBeenCalledWith(false);
        });
    });

    describe('openFullEditor output', () => {
        it('emits when the no-fields button is clicked', () => {
            contentTypeCache.set({ Blog: { layout: [] } });
            spectator = createComponent({
                props: {
                    data: makeData({ contentType: 'Blog', onNumberOfPages: 1 })
                }
            });
            spectator.detectChanges();

            const emitSpy = jest.fn();
            spectator.component.openFullEditor.subscribe(emitSpy);

            const button = spectator
                .query(byTestId('empty-open-full-editor-button'))
                ?.querySelector('button');
            expect(button).toBeTruthy();
            spectator.click(button as HTMLElement);

            expect(emitSpy).toHaveBeenCalled();
        });
    });
});
