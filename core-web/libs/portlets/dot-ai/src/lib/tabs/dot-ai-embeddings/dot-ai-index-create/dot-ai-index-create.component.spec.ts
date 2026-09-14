import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { signal } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotAiIndex } from '@dotcms/dotcms-models';

import { DotAiIndexCreateComponent } from './dot-ai-index-create.component';

import { DOT_AI_INDEX_OPERATION, DotAiIndexNotice } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

const dialogConfig = { closable: true, closeOnEscape: true };

describe('DotAiIndexCreateComponent', () => {
    let spectator: Spectator<DotAiIndexCreateComponent>;
    let dialogRef: DynamicDialogRef;
    let store: {
        indexes: ReturnType<typeof signal<DotAiIndex[]>>;
        indexNotice: ReturnType<typeof signal<DotAiIndexNotice | null>>;
        buildIndex: ReturnType<typeof vi.fn>;
        claimIndexOutcome: ReturnType<typeof vi.fn>;
        releaseIndexOutcome: ReturnType<typeof vi.fn>;
    };

    const createComponent = createComponentFactory({
        component: DotAiIndexCreateComponent,
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotMessageService),
            { provide: DynamicDialogConfig, useValue: dialogConfig }
        ],
        shallow: true
    });

    beforeEach(() => {
        dialogConfig.closable = true;
        dialogConfig.closeOnEscape = true;

        store = {
            indexes: signal<DotAiIndex[]>([
                {
                    name: 'default',
                    fragments: 1,
                    contents: 1,
                    tokenTotal: 1,
                    tokensPerChunk: 1,
                    contentTypes: []
                }
            ]),
            indexNotice: signal<DotAiIndexNotice | null>(null),
            buildIndex: vi.fn(),
            claimIndexOutcome: vi.fn(),
            releaseIndexOutcome: vi.fn()
        };

        spectator = createComponent({
            providers: [{ provide: DotAiStore, useValue: store }]
        });
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    const fill = (testId: string, value: string) =>
        spectator.typeInElement(value, spectator.query(byTestId(testId)) as HTMLElement);

    /** PrimeNG puts its click handler on the inner <button>, not the p-button host. */
    /** PrimeNG binds its Escape listener once at open, so the dialog owns the key itself. */
    const pressEscape = () =>
        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    const clickButton = (testId: string) =>
        spectator.click(
            spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement
        );

    const fillValidForm = () => {
        fill('dotai-index-create-name', 'blogs');
        fill('dotai-index-create-query', '+contentType:Blog');
        spectator.detectChanges();
    };

    it('should keep submit disabled until both name and query are given', () => {
        const submit = () =>
            spectator.query(byTestId('dotai-index-create-submit'))?.querySelector('button');

        expect(submit()?.disabled).toBe(true);

        fill('dotai-index-create-name', 'blogs');
        spectator.detectChanges();
        expect(submit()?.disabled).toBe(true);

        fill('dotai-index-create-query', '+contentType:Blog');
        spectator.detectChanges();
        expect(submit()?.disabled).toBe(false);
    });

    it('should build through the store rather than resolving the dialog', () => {
        fillValidForm();
        fill('dotai-index-create-fields', 'title,body');
        spectator.detectChanges();

        clickButton('dotai-index-create-submit');

        expect(store.buildIndex).toHaveBeenCalledWith({
            indexName: 'blogs',
            query: '+contentType:Blog',
            fields: 'title,body'
        });
        // The whole point: the query has to survive long enough to be corrected.
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should trim whitespace off the name and query', () => {
        fill('dotai-index-create-name', '  blogs  ');
        fill('dotai-index-create-query', '  +contentType:Blog  ');
        spectator.detectChanges();

        clickButton('dotai-index-create-submit');

        expect(store.buildIndex).toHaveBeenCalledWith(
            expect.objectContaining({ indexName: 'blogs', query: '+contentType:Blog' })
        );
    });

    it('should show a rejected query inline and keep what was typed', () => {
        fillValidForm();
        clickButton('dotai-index-create-submit');

        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'failed',
            indexName: 'blogs',
            detail: 'Cannot parse query'
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('dotai-index-create-notice'))).toBeTruthy();
        expect(dialogRef.close).not.toHaveBeenCalled();
        expect(
            (spectator.query(byTestId('dotai-index-create-query')) as HTMLTextAreaElement).value
        ).toBe('+contentType:Blog');
    });

    it('should keep a query that matched nothing in the dialog too', () => {
        fillValidForm();
        clickButton('dotai-index-create-submit');

        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'empty',
            indexName: 'blogs'
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('dotai-index-create-notice'))).toBeTruthy();
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should close itself once the build succeeds', () => {
        fillValidForm();
        clickButton('dotai-index-create-submit');

        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'ok',
            indexName: 'blogs',
            detail: '12'
        });
        spectator.detectChanges();

        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should block a second submit while a build is outstanding', () => {
        fillValidForm();
        clickButton('dotai-index-create-submit');
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('dotai-index-create-submit'))?.querySelector('button')
                ?.disabled
        ).toBe(true);
        expect(store.buildIndex).toHaveBeenCalledTimes(1);
    });

    it('should let the form be submitted again once the build fails', () => {
        fillValidForm();
        clickButton('dotai-index-create-submit');

        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'failed',
            indexName: 'blogs',
            detail: 'bad query'
        });
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('dotai-index-create-submit'))?.querySelector('button')
                ?.disabled
        ).toBe(false);
    });

    it('should reject a name an existing index already uses', () => {
        fill('dotai-index-create-name', 'default');
        spectator.detectChanges();

        expect(spectator.query(byTestId('dotai-index-create-name-error'))).toBeTruthy();
    });

    it('should close with nothing on cancel, so the caller does no work', () => {
        clickButton('dotai-index-create-cancel');

        expect(dialogRef.close).toHaveBeenCalledWith();
    });

    it('should claim its outcome at submit and release it on teardown', () => {
        // The tab reports everything no open dialog claimed. Claiming at submit rather than at
        // render is what keeps it independent of which effect runs first.
        fillValidForm();
        clickButton('dotai-index-create-submit');

        expect(store.claimIndexOutcome).toHaveBeenCalledWith(DOT_AI_INDEX_OPERATION.BUILD, 'blogs');

        spectator.fixture.destroy();

        expect(store.releaseIndexOutcome).toHaveBeenCalled();
    });

    it('should not offer a delete mode any more', () => {
        // Removing content is its own dialog now, opened from the row of the index it acts on.
        expect(spectator.query(byTestId('dotai-index-create-mode'))).toBeFalsy();
    });

    it('should mark the two genuinely-required fields as required', () => {
        // $canSubmit blocks submission without both, so the user got no cue until it failed.
        expect(spectator.query('label[for="dotai-index-name"]')).toHaveClass(
            'p-label-input-required'
        );
        expect(spectator.query('label[for="dotai-index-query"]')).toHaveClass(
            'p-label-input-required'
        );
    });

    it('should guide the Velocity template with a placeholder and a hint', () => {
        // It had neither, and it silently overrides Fields — the one thing nobody could guess.
        const template = spectator.query(
            byTestId('dotai-index-create-template')
        ) as HTMLTextAreaElement;

        expect(template.getAttribute('placeholder')).toBeTruthy();
        expect(spectator.query('label[for="dotai-index-template"]')).toBeTruthy();
    });

    it('should not be dismissible while a build is outstanding', () => {
        // The answer is coming back into this form, so it has to still be here to receive it.
        // PrimeNG drives its header X and Escape straight off the dialog config.
        fillValidForm();
        clickButton('dotai-index-create-submit');

        expect(dialogConfig.closable).toBe(false);
        pressEscape();
        expect(dialogRef.close).not.toHaveBeenCalled();
        expect(
            spectator.query(byTestId('dotai-index-create-cancel'))?.querySelector('button')
                ?.disabled
        ).toBe(true);
    });

    it('should be dismissible again once the build settles', () => {
        fillValidForm();
        clickButton('dotai-index-create-submit');

        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'failed',
            indexName: 'blogs',
            detail: 'bad query'
        });
        spectator.detectChanges();

        expect(dialogConfig.closable).toBe(true);
        pressEscape();
        expect(dialogRef.close).toHaveBeenCalled();
        expect(
            spectator.query(byTestId('dotai-index-create-cancel'))?.querySelector('button')
                ?.disabled
        ).toBe(false);
    });
});
