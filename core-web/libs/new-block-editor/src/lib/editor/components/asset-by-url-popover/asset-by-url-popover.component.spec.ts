import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { describe, expect, it } from 'vitest';

import { Injector, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { AssetByUrlPopoverComponent } from './asset-by-url-popover.component';

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { createTestEditor } from '../../testing/editor.testing';

/**
 * Allowed Blocks gating of the "Add asset by URL" type options.
 *
 * `image` and `video` ARE selectable in Allowed Blocks, so they are correctly gated. `youtube` is
 * NOT — `getEditorBlockOptions()` offers block nodes only — so gating it meant restricting a field
 * to anything at all silently removed the YouTube tab, with no way for the administrator to get it
 * back short of clearing the whole list (#37601, defect B).
 *
 * The three options are built in one array, which is exactly why the `image`/`video` cases below
 * matter as much as the `youtube` one: ungating the third must not loosen the other two.
 */
describe('AssetByUrlPopoverComponent — Allowed Blocks gating', () => {
    let spectator: Spectator<AssetByUrlPopoverComponent>;

    // `allowedBlocks` is read through this closure so a single factory can serve every case:
    // overriding the provider per-test fails once the module has been instantiated.
    let allowedBlocks: string[] | undefined;

    const createComponent = createComponentFactory({
        component: AssetByUrlPopoverComponent,
        shallow: true,
        providers: [
            { provide: DotMessageService, useValue: { get: (key: string) => key } },
            {
                // The `<dot-editor-popover>` shell in this component's template reads
                // `activePopover` directly, so the stub needs the signal as well as the methods.
                provide: EditorPopoverService,
                useValue: {
                    activePopover: signal(null),
                    isOpen: () => false,
                    close: () => null,
                    open: () => null,
                    toggle: () => null
                }
            },
            {
                provide: EditorStore,
                useValue: {
                    isAllowed: (block: string) => !allowedBlocks || allowedBlocks.includes(block)
                }
            }
        ]
    });

    /** `undefined` means an unrestricted field; an array is the stored `allowedBlocks` value. */
    const buildWith = (blocks: string[] | undefined) => {
        allowedBlocks = blocks;
        spectator = createComponent({ detectChanges: false });

        const injector = spectator.inject(Injector);
        spectator.setInput('editor', createTestEditor(injector));
        spectator.detectChanges();
    };

    /** Reads the protected computed the template binds to. */
    const optionFor = (value: string) =>
        (
            spectator.component as unknown as {
                typeOptions: () => { value: string; disabled: boolean }[];
            }
        )
            .typeOptions()
            .find((option) => option.value === value);

    describe('on a restricted field', () => {
        it('offers YouTube even though the field is restricted to something else entirely', () => {
            buildWith(['bulletList']);

            expect(optionFor('youtube')?.disabled).toBe(false);
        });

        it('still hides image and video when they are not allowed', () => {
            // The regression that matters: all three options live in one array, so ungating
            // `youtube` must not loosen the two gates that are legitimate.
            buildWith(['bulletList']);

            expect(optionFor('image')?.disabled).toBe(true);
            expect(optionFor('video')?.disabled).toBe(true);
        });

        it('offers image and video when they ARE allowed', () => {
            buildWith(['image', 'video']);

            expect(optionFor('image')?.disabled).toBe(false);
            expect(optionFor('video')?.disabled).toBe(false);
        });

        it('offers YouTube regardless of which blocks are allowed', () => {
            // `youtube` is not producible by the Settings tab, so no configuration should be able
            // to switch it off. Asserting across several restrictions pins that.
            for (const allowed of [['image'], ['table'], ['codeBlock', 'blockquote']]) {
                buildWith(allowed);
                expect(optionFor('youtube')?.disabled).toBe(false);
            }
        });
    });

    describe('on an unrestricted field', () => {
        it('leaves every option enabled, as before', () => {
            // Empty `allowedBlocks` => `allowedBlocksSet` is null => every gate passes
            // (editor.store.ts). This is the path that always worked and must not change.
            buildWith(undefined);

            expect(optionFor('image')?.disabled).toBe(false);
            expect(optionFor('video')?.disabled).toBe(false);
            expect(optionFor('youtube')?.disabled).toBe(false);
        });
    });
});
