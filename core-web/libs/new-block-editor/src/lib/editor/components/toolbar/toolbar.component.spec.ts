import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Injector } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { EditorToolbarStore } from './editor-toolbar.store';
import { ToolbarComponent } from './toolbar.component';

import { EditorModalService } from '../../services/editor-modal.service';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { createTestEditor } from '../../testing/editor.testing';
import { createToolbarHarness } from '../../testing/toolbar.testing';

import type { ContentletEditEvent } from '../../extensions/nodes/contentlet/contentlet.types';

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
    const { buildWith, buttonWithIcon } = createToolbarHarness();

    const emojiButton = () => buttonWithIcon('emoji_emotions');

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

/**
 * #36351 — hyperlink authoring is available on EVERY Block Editor field.
 *
 * Same shape as the emoji regression above, and the last of the three keys #37175 named.
 * `link` is not selectable in Allowed Blocks — `getEditorBlockOptions()` offers block nodes
 * only — so `isAllowed('link')` was true only on a field with NO restriction. Restricting any
 * block at all silently removed the link button, with no admin having chosen it and no control
 * anywhere in the product to undo it. The reported workaround was to clear the whole list and
 * give up restriction entirely.
 *
 * Nothing covered the link button's visibility under any `allowedBlocks` value before this,
 * which is why the defect shipped. These tests exist so the gate cannot come back.
 */
describe('ToolbarComponent — the link button is never gated (#36351)', () => {
    const { buildWith, buttonWithIcon } = createToolbarHarness();

    const linkButton = () => buttonWithIcon('link');

    it('renders the link button on an UNRESTRICTED field', () => {
        buildWith(undefined);

        expect(linkButton()).toBeTruthy();
    });

    /**
     * The regression this guards, and the exact configuration from the customer report:
     * a handful of blocks selected, hyperlinks gone.
     */
    it('renders the link button on a field restricted to a few blocks', () => {
        buildWith(['bulletList', 'blockquote', 'image']);

        expect(linkButton()).toBeTruthy();
    });

    it('renders the link button on a field restricted to a single block', () => {
        buildWith(['bulletList']);

        expect(linkButton()).toBeTruthy();
    });

    /**
     * A stored `allowedBlocks` value containing `link` is writable through the field-variable
     * API even though the settings UI never offers it. It must be inert, not special.
     */
    it('behaves identically when allowedBlocks explicitly contains link', () => {
        buildWith(['bulletList', 'link']);

        expect(linkButton()).toBeTruthy();
    });

    /**
     * FR-006 — ungating hyperlinks must not loosen any OTHER gate. Removing a condition is a
     * cheap edit to get wrong in the neighbouring direction: the link button sits in the same
     * insert group as image, video and table, all guarded by their own `@if`. This asserts the
     * restriction still restricts everything it is supposed to.
     */
    it('leaves every other gate intact on a restricted field', () => {
        buildWith(['bulletList']);

        expect(linkButton()).toBeTruthy();

        // `image` and `table` ARE selectable in Allowed Blocks, so excluding them is a real
        // administrator choice and must still be honoured.
        expect(buttonWithIcon('image')).toBeFalsy();
        expect(buttonWithIcon('table')).toBeFalsy();
        // `code_blocks`, not `code` — the latter is the inline code mark, which is never gated.
        expect(buttonWithIcon('code_blocks')).toBeFalsy();
    });
});

/**
 * #38303 (R8) — editing an embedded contentlet only asks. The toolbar emits `contentletEdit` and
 * the host of the editor decides where the content opens (a stacked side panel). It no longer
 * prompts about unsaved changes, writes a "return to" breadcrumb or navigates the parent window,
 * all of which threw the author out of the content they were editing.
 */
describe('ToolbarComponent — editing an embedded contentlet', () => {
    const EMBEDDED: ContentletEditEvent = {
        identifier: 'embedded-id',
        inode: 'embedded-inode',
        contentType: 'Blog',
        title: 'Embedded blog'
    };

    let spectator: Spectator<ToolbarComponent>;

    /**
     * Accepts whatever it is asked, as an author clicking "Continue" would, so a navigation behind
     * a prompt shows up in these tests instead of hiding behind an unanswered one.
     */
    const confirm = vi.fn((options: { accept?: () => void }) => options.accept?.());

    /** Stands in for the parent window, whose location the toolbar must leave alone. */
    const parentWindow = { location: { href: 'http://localhost/dotAdmin/#/content/parent' } };

    const createComponent = createComponentFactory({
        component: ToolbarComponent,
        shallow: true,
        providers: [
            { provide: DotMessageService, useValue: { get: (key: string) => key } },
            { provide: EditorPopoverService, useValue: { isOpen: () => false, toggle: vi.fn() } },
            { provide: EditorModalService, useValue: {} },
            { provide: ConfirmationService, useValue: { confirm } },
            {
                provide: EditorStore,
                useValue: { isAllowed: () => true, allowedBlocksSet: () => new Set() }
            },
            {
                provide: EditorToolbarStore,
                // Everything answers "inactive", except the contentlet the author selected.
                useValue: new Proxy(
                    {},
                    {
                        get: (_target, property) => {
                            if (property === 'connect') {
                                return vi.fn().mockReturnValue(() => undefined);
                            }

                            return property === 'selectedContentlet' ? () => EMBEDDED : () => false;
                        }
                    }
                )
            }
        ]
    });

    beforeEach(() => {
        confirm.mockClear();
        parentWindow.location.href = 'http://localhost/dotAdmin/#/content/parent';
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parentWindow as unknown as Window);

        spectator = createComponent({ detectChanges: false });
        spectator.setInput('editor', createTestEditor(spectator.inject(Injector)));
        spectator.detectChanges();
    });

    afterEach(() => {
        spectator?.component?.editor()?.destroy();
        vi.restoreAllMocks();
    });

    /** Clicks the toolbar's "edit contentlet" button. */
    const editContentlet = () =>
        spectator.dispatchMouseEvent(byTestId('toolbar-edit-contentlet'), 'mousedown');

    it('emits contentletEdit with the selected contentlet', () => {
        const emitted = vi.fn();
        spectator.output('contentletEdit').subscribe(emitted);

        editContentlet();

        expect(emitted).toHaveBeenCalledWith(EMBEDDED);
    });

    it('does not prompt about unsaved changes', () => {
        editContentlet();

        expect(confirm).not.toHaveBeenCalled();
    });

    it('does not navigate the parent window', () => {
        editContentlet();

        expect(parentWindow.location.href).toBe('http://localhost/dotAdmin/#/content/parent');
    });

    it('does not write a "return to" breadcrumb to localStorage', () => {
        const setItem = vi.spyOn(Storage.prototype, 'setItem');

        editContentlet();

        expect(setItem).not.toHaveBeenCalled();
    });
});
