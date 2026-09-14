import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { signal } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotAiIndexRemoveContentComponent } from './dot-ai-index-remove-content.component';

import { DOT_AI_INDEX_OPERATION, DotAiIndexNotice } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

describe('DotAiIndexRemoveContentComponent', () => {
    let spectator: Spectator<DotAiIndexRemoveContentComponent>;
    let dialogRef: DynamicDialogRef;
    let store: {
        indexNotice: ReturnType<typeof signal<DotAiIndexNotice | null>>;
        removeFromIndex: ReturnType<typeof vi.fn>;
        dismissIndexNotice: ReturnType<typeof vi.fn>;
    };

    const createComponent = createComponentFactory({
        component: DotAiIndexRemoveContentComponent,
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotMessageService, { get: (key: string) => key }),
            { provide: DynamicDialogConfig, useValue: { data: { indexName: 'blogs' } } }
        ],
        shallow: true
    });

    beforeEach(() => {
        store = {
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

    it('should take the index from the row rather than asking for it', () => {
        // It used to be a free-text field on the build dialog: a typo removed nothing and a
        // near-miss hit a different index, both silently.
        expect(spectator.query(byTestId('dotai-index-create-name'))).toBeFalsy();
        expect(spectator.query(byTestId('dotai-index-remove-explainer'))).toContainText(
            'dotai.embeddings.remove-content.explainer'
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
