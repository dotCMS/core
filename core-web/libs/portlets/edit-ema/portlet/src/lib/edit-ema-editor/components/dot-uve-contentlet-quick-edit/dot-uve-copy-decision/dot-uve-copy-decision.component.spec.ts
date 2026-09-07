import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotCopyContentService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotUveCopyDecisionComponent } from './dot-uve-copy-decision.component';

import { UVEStore } from '../../../../store/dot-uve.store';
import { ContentletEditData } from '../types';

const makeContentlet = (overrides: Partial<DotCMSContentlet> = {}): DotCMSContentlet =>
    ({
        identifier: 'id-1',
        inode: 'inode-1',
        contentType: 'Blog',
        ...overrides
    }) as DotCMSContentlet;

const makeData = (overrides: Partial<DotCMSContentlet> = {}): ContentletEditData => ({
    container: { identifier: 'c-1', uuid: '1' } as ContentletEditData['container'],
    contentlet: makeContentlet(overrides)
});

describe('DotUveCopyDecisionComponent', () => {
    let spectator: Spectator<DotUveCopyDecisionComponent>;
    let copyInPage: jest.Mock;
    let getCurrentTreeNode: jest.Mock;
    let getPageSavePayload: jest.Mock;
    let setSelectedPayload: jest.Mock;
    let pageReload: jest.Mock;
    let handleHttpError: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotUveCopyDecisionComponent,
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    getCurrentTreeNode,
                    getPageSavePayload,
                    setSelectedPayload,
                    pageReload
                })
            },
            {
                provide: DotCopyContentService,
                useFactory: () => ({ copyInPage })
            },
            {
                provide: DotHttpErrorManagerService,
                useFactory: () => ({ handle: handleHttpError })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        copyInPage = jest.fn().mockReturnValue(of({}));
        getCurrentTreeNode = jest.fn().mockReturnValue({ pageId: 'p1' });
        getPageSavePayload = jest.fn().mockReturnValue({ payload: 'ok' });
        setSelectedPayload = jest.fn();
        pageReload = jest.fn();
        handleHttpError = jest.fn().mockReturnValue(of(null));
    });

    it('renders both decision buttons', () => {
        spectator = createComponent({ props: { data: makeData() } });
        spectator.detectChanges();

        expect(spectator.query(byTestId('copy-mode-all-pages'))).toBeTruthy();
        expect(spectator.query(byTestId('copy-mode-this-page'))).toBeTruthy();
    });

    describe('ALL_PAGES branch', () => {
        it('emits decisionMade and does not call copyInPage', () => {
            spectator = createComponent({ props: { data: makeData() } });
            spectator.detectChanges();

            const emitSpy = jest.fn();
            spectator.component.decisionMade.subscribe(emitSpy);

            spectator.click(byTestId('copy-mode-all-pages'));

            expect(emitSpy).toHaveBeenCalled();
            expect(copyInPage).not.toHaveBeenCalled();
            expect(setSelectedPayload).not.toHaveBeenCalled();
            expect(pageReload).not.toHaveBeenCalled();
        });
    });

    describe('THIS_PAGE branch', () => {
        it('forks the contentlet, patches selection, and reloads the page', () => {
            const copied = {
                identifier: 'forked-id',
                inode: 'forked-inode',
                title: 'Forked',
                contentType: 'Blog'
            } as DotCMSContentlet;
            copyInPage.mockReturnValue(of(copied));

            spectator = createComponent({ props: { data: makeData() } });
            spectator.detectChanges();

            const emitSpy = jest.fn();
            spectator.component.decisionMade.subscribe(emitSpy);

            spectator.click(byTestId('copy-mode-this-page'));

            expect(copyInPage).toHaveBeenCalledWith({ pageId: 'p1' });
            expect(getPageSavePayload).toHaveBeenCalledWith({
                container: expect.anything(),
                contentlet: expect.objectContaining({
                    identifier: 'forked-id',
                    inode: 'forked-inode',
                    title: 'Forked',
                    onNumberOfPages: 1
                })
            });
            expect(setSelectedPayload).toHaveBeenCalledWith({ payload: 'ok' });
            expect(pageReload).toHaveBeenCalled();
            // THIS_PAGE branch resolves through pageReload, not a decisionMade emission.
            expect(emitSpy).not.toHaveBeenCalled();
        });

        it('routes errors through DotHttpErrorManagerService instead of throwing', () => {
            const error = new Error('boom');
            copyInPage.mockReturnValue(throwError(() => error));

            spectator = createComponent({ props: { data: makeData() } });
            spectator.detectChanges();

            spectator.click(byTestId('copy-mode-this-page'));

            expect(handleHttpError).toHaveBeenCalledWith(error);
            expect(setSelectedPayload).not.toHaveBeenCalled();
            expect(pageReload).not.toHaveBeenCalled();
        });
    });

    describe('reset on contentlet change', () => {
        it('clears the selected mode when the identifier changes', () => {
            // First contentlet — pick ALL_PAGES, which only sets the
            // local mode signal (no async work).
            spectator = createComponent({
                props: { data: makeData({ identifier: 'id-1' }) }
            });
            spectator.detectChanges();
            spectator.click(byTestId('copy-mode-all-pages'));
            spectator.detectChanges();

            expect(
                spectator
                    .query(byTestId('copy-mode-all-pages'))
                    ?.classList.contains('border-primary-500')
            ).toBe(true);

            spectator.setInput('data', makeData({ identifier: 'id-2' }));
            spectator.detectChanges();

            expect(
                spectator
                    .query(byTestId('copy-mode-all-pages'))
                    ?.classList.contains('border-primary-500')
            ).toBe(false);
        });
    });
});
