import { createToolbarHarness } from '../../testing/toolbar.testing';

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
 * The "Add asset by URL" trigger appears when the popover can offer at least one asset type.
 *
 * `image` and `video` are producible by the Settings tab and correctly gated. `youtube` is not —
 * `getEditorBlockOptions()` offers block nodes only — so including it in the trigger's condition
 * meant the trigger showed on a field that allows nothing relevant, while the YouTube tab inside
 * the popover was disabled anyway (#37601, defect B). Ungating `youtube` makes the trigger honest:
 * it is always available, because YouTube always is.
 */
describe('ToolbarComponent — the asset-by-URL trigger never depends on youtube (#37601)', () => {
    const { buildWith, buttonWithIcon } = createToolbarHarness();

    // `media_link`, not `link` — the latter is the hyperlink button. The harness matches the icon
    // exactly for this reason.
    const assetByUrlButton = () => buttonWithIcon('media_link');

    it('shows on an unrestricted field', () => {
        buildWith(undefined);

        expect(assetByUrlButton()).toBeTruthy();
    });

    it('shows when image or video is allowed', () => {
        buildWith(['image']);

        expect(assetByUrlButton()).toBeTruthy();
    });

    it('shows even when neither image nor video is allowed — YouTube is always on offer', () => {
        buildWith(['bulletList']);

        expect(assetByUrlButton()).toBeTruthy();
    });

    // Pinning the contract rather than the symptom: a field that lists `youtube` explicitly is not
    // a configuration anyone can create, so it must behave identically to one that does not. Two
    // separate cases because the harness overrides providers per build and cannot be rebuilt
    // inside a single test.
    it('behaves identically when allowedBlocks explicitly contains youtube', () => {
        buildWith(['bulletList', 'youtube']);

        expect(assetByUrlButton()).toBeTruthy();
    });

    it('behaves identically when allowedBlocks omits youtube', () => {
        buildWith(['bulletList']);

        expect(assetByUrlButton()).toBeTruthy();
    });
});
