import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Injector } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { EditorToolbarStore } from './editor-toolbar.store';
import { ToolbarComponent } from './toolbar.component';

import { ContentletEditUrlService } from '../../services/contentlet-edit-url.service';
import { EditorModalService } from '../../services/editor-modal.service';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { createTestEditor } from '../../testing/editor.testing';

/**
 * #37340 AC-008 — emoji authoring is available on EVERY Block Editor field.
 *
 * `emoji` is not selectable in Allowed Blocks: the option list comes from
 * `getEditorBlockOptions()`, which offers block nodes only, and `link`/`emoji`/`youtube` were
 * excluded by #37175. So `isAllowed('emoji')` was true only on a field with NO restriction —
 * restricting any block at all silently removed the emoji button, with no admin having chosen it.
 *
 * These tests exist so that gate cannot come back. They restrict the field hard and assert the
 * button is still there.
 */
describe('ToolbarComponent — emoji is never gated (#37340)', () => {
    let spectator: Spectator<ToolbarComponent>;
    let injector: Injector;

    const createComponent = createComponentFactory({
        component: ToolbarComponent,
        shallow: true,
        providers: [
            { provide: DotMessageService, useValue: { get: (key: string) => key } },
            { provide: EditorPopoverService, useValue: { isOpen: () => false, toggle: jest.fn() } },
            { provide: EditorModalService, useValue: {} },
            { provide: ContentletEditUrlService, useValue: {} },
            { provide: ConfirmationService, useValue: {} },
            {
                provide: EditorToolbarStore,
                /**
                 * A Proxy rather than a hand-listed stub. The toolbar reads a wide surface of
                 * mark/block/alignment signals, and enumerating them would make this spec a
                 * maintenance burden for every future toolbar button while testing none of them.
                 * Everything answers "inactive"; the one thing under test is whether the emoji
                 * button renders at all.
                 */
                useValue: new Proxy(
                    {},
                    {
                        get: (_target, property) =>
                            property === 'connect'
                                ? jest.fn().mockReturnValue(() => undefined)
                                : () => false
                    }
                )
            }
        ]
    });

    const buildWith = (allowedBlocks: string[] | undefined) => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: EditorStore,
                    useValue: {
                        // The only store surface the toolbar template reads for gating.
                        isAllowed: (block: string) =>
                            !allowedBlocks || allowedBlocks.includes(block),
                        allowedBlocksSet: () => new Set(allowedBlocks ?? [])
                    }
                }
            ]
        });

        injector = spectator.inject(Injector);
        spectator.setInput('editor', createTestEditor(injector));
        spectator.detectChanges();
    };

    afterEach(() => {
        spectator?.component?.editor()?.destroy();
    });

    const emojiButton = () =>
        spectator
            .queryAll('button')
            .find((button) =>
                button
                    .querySelector('.material-symbols-outlined')
                    ?.textContent?.includes('emoji_emotions')
            );

    it('renders the emoji button on an UNRESTRICTED field', () => {
        buildWith(undefined);

        expect(emojiButton()).toBeTruthy();
    });

    it('renders the emoji button on a field restricted to headings only', () => {
        buildWith(['heading1', 'heading2']);

        expect(emojiButton()).toBeTruthy();
    });

    /**
     * The regression this guards. Before #37340 the field below lost its emoji button entirely,
     * because restricting anything made `isAllowed('emoji')` false.
     */
    it('renders the emoji button on a field restricted to a single block', () => {
        buildWith(['bulletList']);

        expect(emojiButton()).toBeTruthy();
    });

    /**
     * AC-009 — a stored `allowedBlocks` value that happens to contain `emoji` is writable through
     * the field-variable API even though the settings UI never offers it. It must be inert, not an
     * error, and must not change anything.
     */
    it('behaves identically when allowedBlocks explicitly contains emoji', () => {
        buildWith(['bulletList', 'emoji']);

        expect(emojiButton()).toBeTruthy();
    });
});
