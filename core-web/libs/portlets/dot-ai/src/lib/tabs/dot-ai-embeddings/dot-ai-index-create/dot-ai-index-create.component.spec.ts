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

import { DotAiIndexCreateComponent } from './dot-ai-index-create.component';

import { DOT_AI_INDEX_OPERATION, DotAiIndexNotice } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

describe('DotAiIndexCreateComponent', () => {
    let spectator: Spectator<DotAiIndexCreateComponent>;
    let dialogRef: DynamicDialogRef;
    let store: {
        indexes: ReturnType<typeof signal<DotAiIndex[]>>;
        indexNotice: ReturnType<typeof signal<DotAiIndexNotice | null>>;
        buildIndex: ReturnType<typeof vi.fn>;
        dismissIndexNotice: ReturnType<typeof vi.fn>;
    };

    const createComponent = createComponentFactory({
        component: DotAiIndexCreateComponent,
        providers: [mockProvider(DynamicDialogRef), mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
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
            dismissIndexNotice: vi.fn()
        };

        spectator = createComponent({
            providers: [{ provide: DotAiStore, useValue: store }]
        });
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    const fill = (testId: string, value: string) =>
        spectator.typeInElement(value, spectator.query(byTestId(testId)) as HTMLElement);

    /** PrimeNG puts its click handler on the inner <button>, not the p-button host. */
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

    it('should leave an unshown outcome standing for the tab to report', () => {
        // PrimeNG's own header X and Escape call DynamicDialogRef.close directly, so a build
        // abandoned mid-flight used to fail into silence. The tab toasts whatever this dialog
        // was not around to render, which means the notice has to survive its teardown.
        store.indexNotice.set({
            operation: DOT_AI_INDEX_OPERATION.BUILD,
            outcome: 'failed',
            indexName: 'blogs'
        });
        spectator.fixture.destroy();

        expect(store.dismissIndexNotice).not.toHaveBeenCalled();
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
});
