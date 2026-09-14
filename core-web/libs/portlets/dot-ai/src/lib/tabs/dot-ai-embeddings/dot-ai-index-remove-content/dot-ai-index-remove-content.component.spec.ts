import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { signal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotAiIndex } from '@dotcms/dotcms-models';

import { DotAiIndexRemoveContentComponent } from './dot-ai-index-remove-content.component';

import { DOT_AI_INDEX_OPERATION, DotAiIndexNotice } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

describe('DotAiIndexRemoveContentComponent', () => {
    let spectator: Spectator<DotAiIndexRemoveContentComponent>;
    let dialogRef: DynamicDialogRef;
    let store: {
        indexes: ReturnType<typeof signal<DotAiIndex[]>>;
        indexNotice: ReturnType<typeof signal<DotAiIndexNotice | null>>;
        removeFromIndex: ReturnType<typeof vi.fn>;
        dismissIndexNotice: ReturnType<typeof vi.fn>;
    };

    const createComponent = createComponentFactory({
        component: DotAiIndexRemoveContentComponent,
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotMessageService, { get: (key: string) => key })
        ],
        shallow: true
    });

    beforeEach(() => {
        const index = (name: string): DotAiIndex => ({
            name,
            fragments: 1,
            contents: 1,
            tokenTotal: 1,
            tokensPerChunk: 1,
            contentTypes: []
        });

        store = {
            indexes: signal<DotAiIndex[]>([index('blogs'), index('product')]),
            indexNotice: signal<DotAiIndexNotice | null>(null),
            removeFromIndex: vi.fn(),
            dismissIndexNotice: vi.fn()
        };

        spectator = createComponent({ providers: [{ provide: DotAiStore, useValue: store }] });
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    /** PrimeNG puts its click handler on the inner <button>, not the p-button host. */
    const clickButton = (testId: string) =>
        spectator.click(
            spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement
        );

    const fillQuery = (value: string) => {
        spectator.typeInElement(
            value,
            spectator.query(byTestId('dotai-index-remove-query')) as HTMLElement
        );
        spectator.detectChanges();
    };

    const notice = (outcome: DotAiIndexNotice['outcome'], detail?: string): DotAiIndexNotice => ({
        operation: DOT_AI_INDEX_OPERATION.REMOVE_CONTENT,
        outcome,
        indexName: 'blogs',
        detail
    });

    it('should offer the real indexes rather than a free-text name', () => {
        // As a free-text field on the old delete mode, a typo removed nothing and a near-miss
        // hit a different index, both silently.
        expect(spectator.query(byTestId('dotai-index-remove-index'))).toBeTruthy();
        expect(spectator.query('input[type="text"]#dotai-index-remove-index')).toBeFalsy();
        expect(
            (spectator.component as unknown as { $indexOptions: () => string[] }).$indexOptions()
        ).toEqual(['blogs', 'product']);
    });

    it('should explain that it removes embeddings rather than content', () => {
        expect(spectator.query(byTestId('dotai-index-remove-explainer'))).toContainText(
            'dotai.embeddings.remove-content.explainer'
        );
    });

    it('should default to the first index so the form is usable straight away', () => {
        fillQuery('+contentType:Blog');

        clickButton('dotai-index-remove-submit');

        expect(store.removeFromIndex).toHaveBeenCalledWith(
            expect.objectContaining({ indexName: 'blogs' })
        );
    });

    it('should keep submit disabled until a query is given', () => {
        const submit = () =>
            spectator.query(byTestId('dotai-index-remove-submit'))?.querySelector('button');

        expect(submit()?.disabled).toBe(true);

        fillQuery('+contentType:Blog');

        expect(submit()?.disabled).toBe(false);
    });

    it('should remove through the store rather than resolving the dialog', () => {
        fillQuery('  +contentType:Blog  ');

        clickButton('dotai-index-remove-submit');

        expect(store.removeFromIndex).toHaveBeenCalledWith({
            indexName: 'blogs',
            query: '+contentType:Blog'
        });
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should keep a query that matched nothing on screen to be corrected', () => {
        // The server answers 200 with `deleted: 0`, which would otherwise read as a success.
        fillQuery('+contentType:NoSuchType');
        clickButton('dotai-index-remove-submit');

        store.indexNotice.set(notice('empty', '0'));
        spectator.detectChanges();

        expect(spectator.query(byTestId('dotai-index-remove-notice'))).toBeTruthy();
        expect(dialogRef.close).not.toHaveBeenCalled();
        expect(
            (spectator.query(byTestId('dotai-index-remove-query')) as HTMLTextAreaElement).value
        ).toBe('+contentType:NoSuchType');
    });

    it('should show a rejected query inline', () => {
        fillQuery('+++[');
        clickButton('dotai-index-remove-submit');

        store.indexNotice.set(notice('failed', 'Cannot parse query'));
        spectator.detectChanges();

        expect(spectator.query(byTestId('dotai-index-remove-notice'))).toBeTruthy();
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should close itself once something was actually removed', () => {
        fillQuery('+contentType:Blog');
        clickButton('dotai-index-remove-submit');

        store.indexNotice.set(notice('ok', '3'));
        spectator.detectChanges();

        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should ignore an outcome belonging to another operation', () => {
        // One notice channel serves all four operations, so each dialog has to read only its own.
        fillQuery('+contentType:Blog');
        clickButton('dotai-index-remove-submit');

        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'failed',
            indexName: 'other'
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('dotai-index-remove-notice'))).toBeFalsy();
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should block a second submit while a removal is outstanding', () => {
        fillQuery('+contentType:Blog');
        clickButton('dotai-index-remove-submit');
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('dotai-index-remove-submit'))?.querySelector('button')
                ?.disabled
        ).toBe(true);
        expect(store.removeFromIndex).toHaveBeenCalledTimes(1);
    });
});
