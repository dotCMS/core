import { ComponentFixture, TestBed } from '@angular/core/testing';

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import type { BlockEditorNode } from '@dotcms/types';

import { DotCMSBlockEditorRendererNativeComponent } from './dotcms-block-editor-renderer-native.component';

import { DotCMSBlockEditorRendererComponent } from '../dotcms-block-editor-renderer/dotcms-block-editor-renderer.component';

const fixtures = JSON.parse(
    readFileSync(
        resolve(__dirname, '../../../../../types/src/lib/testing/story-block.fixtures.json'),
        'utf8'
    )
);

const doc = (key: string): BlockEditorNode => ({
    type: 'doc',
    content: [{ type: 'paragraph', content: fixtures[key].content }]
});

/**
 * Both Angular renderers, one spec. The SDK ships two independent Block Editor renderers with
 * separate dispatch and mark logic, and #37340 never mentioned the second one — exactly the kind
 * of gap that silently ships half a fix.
 */
describe.each([
    ['semantic (native)', DotCMSBlockEditorRendererNativeComponent, true],
    ['standard (deprecated)', DotCMSBlockEditorRendererComponent, false]
])('Angular %s renderer — emoji + link runs (#37340)', (_label, Cmp, exactText) => {
    let fixture: ComponentFixture<InstanceType<typeof Cmp>>;

    /**
     * The deprecated renderer wraps every semantic tag in a custom element, and Angular emits
     * whitespace text nodes between them — its own JSDoc calls that out. Structure (anchor
     * counts, nesting) is asserted identically for both; only exact character-level text is
     * relaxed there, since the extra whitespace predates this change.
     */
    const text = (el: Element | null) =>
        exactText ? (el?.textContent ?? '') : (el?.textContent ?? '').replace(/\s+/g, ' ').trim();

    const renderDoc = (key: string): HTMLElement => {
        fixture = TestBed.createComponent(Cmp) as ComponentFixture<InstanceType<typeof Cmp>>;
        fixture.componentRef.setInput('blocks', doc(key));
        fixture.detectChanges();

        return fixture.nativeElement as HTMLElement;
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [Cmp] }).compileComponents();
    });

    it('AC-010: renders the character for a stored emoji node', () => {
        expect(text(renderDoc('mixedRepresentation'))).toContain(
            exactText
                ? fixtures['mixedRepresentation'].expectedText
                : fixtures['mixedRepresentation'].expectedText.replace(/\s+/g, ' ').trim()
        );
    });

    it('AC-015: Shape 1 — a link split by an unmarked emoji renders as ONE anchor', () => {
        const links = renderDoc('shape1_emojiSplitsLink').querySelectorAll('a');

        expect(links).toHaveLength(1);
        if (exactText) {
            expect(links[0].textContent).toBe(fixtures['shape1_emojiSplitsLink'].expectedText);
        } else {
            // The deprecated renderer emits a whitespace text node between the custom elements
            // that wrap each node, so `…Copyright ©All…` comes out as `…Copyright © All…`. That
            // spacing predates this change and is inherent to the wrapping its own JSDoc
            // deprecates. Assert what matters instead: the emoji is INSIDE the single anchor.
            expect(text(links[0])).toContain('dotCMS Copyright');
            expect(text(links[0])).toContain('©');
            expect(text(links[0])).toContain('All rights reserved');
        }
    });

    it('Shape 2 — an emoji already carrying the link stays in one anchor', () => {
        expect(renderDoc('shape2_emojiCarriesLink').querySelectorAll('a')).toHaveLength(1);
    });

    it('AC-016: a non-emoji atom breaks the run', () => {
        expect(renderDoc('nonEmojiAtomBreaksRun').querySelectorAll('a')).toHaveLength(2);
    });

    it('AC-017: differing link attrs stay separate anchors', () => {
        expect(renderDoc('differingLinkAttrs').querySelectorAll('a')).toHaveLength(2);
    });

    it('AC-018: bold still nests inside the single anchor', () => {
        const el = renderDoc('boldNestedInLink');

        expect(el.querySelectorAll('a')).toHaveLength(1);
        expect(el.querySelector('a strong')).not.toBeNull();
    });

    it('AC-013: an unresolvable name renders escaped, never as live markup', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const el = renderDoc('unresolvableNameWithHtml');

        expect(el.querySelector('img')).toBeNull();
        expect(text(el)).toContain('<img src=x onerror=alert(1)>');

        warn.mockRestore();
    });

    it('does not wrap unlinked content in an anchor', () => {
        expect(renderDoc('noOp').querySelectorAll('a')).toHaveLength(0);
    });
});
