import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { DotKeyboardShortcutService } from './dot-keyboard-shortcut.service';
import { DotKeyboardShortcut, DotKeyboardShortcutUnregister } from './models';

describe('DotKeyboardShortcutService', () => {
    let spectator: SpectatorService<DotKeyboardShortcutService>;
    let service: DotKeyboardShortcutService;

    const createService = createServiceFactory(DotKeyboardShortcutService);

    /**
     * `jest` here comes from `@jest/globals`, so a bare `jest.fn()` is `Mock<UnknownFunction>` and
     * returns `unknown`, which is not assignable to a handler's `boolean | void`.
     */
    const handlerMock = () => jest.fn<(event: KeyboardEvent) => boolean | void>();

    /**
     * Registrations made through this are withdrawn after every test.
     *
     * The service listens on the document, so a claim left standing outlives its test: the stale
     * listener consumes the event and marks it handled, and the next test's service then correctly
     * ignores it. That produced a very convincing set of false failures before this was added.
     */
    let registered: DotKeyboardShortcutUnregister[] = [];

    const register = (
        shortcuts: DotKeyboardShortcut | DotKeyboardShortcut[]
    ): DotKeyboardShortcutUnregister => {
        const unregister = service.register(shortcuts);
        registered.push(unregister);

        return unregister;
    };

    /** Dispatches a real keydown on the document, the way a browser would. */
    const press = (init: KeyboardEventInit): KeyboardEvent => {
        const event = new KeyboardEvent('keydown', { bubbles: true, cancelable: true, ...init });
        document.dispatchEvent(event);

        return event;
    };

    const pressModK = () => press({ key: 'k', metaKey: true });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        registered = [];
    });

    afterEach(() => {
        registered.forEach((unregister) => unregister());
        registered = [];
    });

    describe('registration and dispatch', () => {
        it('should run the handler for a claimed combination', () => {
            const handler = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler });

            pressModK();

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it('should not run the handler for a different combination', () => {
            const handler = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler });

            press({ key: 'j', metaKey: true });

            expect(handler).not.toHaveBeenCalled();
        });

        it('should treat Control as the primary modifier too, so one claim serves every platform', () => {
            const handler = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler });

            press({ key: 'k', ctrlKey: true });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it('should not run a modifier combination when the modifier is absent', () => {
            const handler = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler });

            press({ key: 'k' });

            expect(handler).not.toHaveBeenCalled();
        });

        it('should distinguish a shift-modified combination from the plain one', () => {
            const plain = handlerMock();
            const shifted = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler: plain });
            register({ combination: 'shift+mod+k', label: 'other', handler: shifted });

            press({ key: 'k', metaKey: true, shiftKey: true });

            expect(shifted).toHaveBeenCalledTimes(1);
            expect(plain).not.toHaveBeenCalled();
        });

        it('should support a combination with no modifier', () => {
            const handler = handlerMock();
            register({ combination: 'escape', label: 'dismiss', handler });

            press({ key: 'Escape' });

            expect(handler).toHaveBeenCalledTimes(1);
        });
    });

    describe('last-in-wins arbitration', () => {
        it('should give the key to the most recent claim on that combination', () => {
            const first = handlerMock();
            const second = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler: first });
            register({ combination: 'mod+k', label: 'search', handler: second });

            pressModK();

            expect(second).toHaveBeenCalledTimes(1);
            expect(first).not.toHaveBeenCalled();
        });

        it('should restore the previous claim when the newer one is withdrawn', () => {
            const first = handlerMock();
            const second = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler: first });
            const unregister = register({
                combination: 'mod+k',
                label: 'search',
                handler: second
            });

            unregister();
            pressModK();

            expect(first).toHaveBeenCalledTimes(1);
            expect(second).not.toHaveBeenCalled();
        });

        it('should leave a combination unclaimed once every claim is withdrawn', () => {
            const handler = handlerMock();
            const unregister = register({
                combination: 'mod+k',
                label: 'search',
                handler
            });

            unregister();
            const event = pressModK();

            expect(handler).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(false);
        });

        it('should tolerate withdrawing the same registration twice', () => {
            const handler = handlerMock();
            const unregister = register({
                combination: 'mod+k',
                label: 'search',
                handler
            });

            unregister();
            unregister();

            expect(() => pressModK()).not.toThrow();
        });

        it('should not disturb another combination when one is withdrawn', () => {
            const search = handlerMock();
            const dismiss = handlerMock();
            const unregisterSearch = register({
                combination: 'mod+k',
                label: 'search',
                handler: search
            });
            register({ combination: 'escape', label: 'dismiss', handler: dismiss });

            unregisterSearch();
            press({ key: 'Escape' });

            expect(dismiss).toHaveBeenCalledTimes(1);
        });

        // The case that rules out "topmost surface wins outright": a surface opening over another
        // must not swallow combinations it never claimed.
        it('should leave a combination with the earlier claimant when a newer surface claims a different one', () => {
            const portletEscape = handlerMock();
            const dialogSearch = handlerMock();
            register({ combination: 'escape', label: 'dismiss', handler: portletEscape });
            register({ combination: 'mod+k', label: 'search', handler: dialogSearch });

            press({ key: 'Escape' });

            expect(portletEscape).toHaveBeenCalledTimes(1);
        });
    });

    // A surface almost always wants several combinations at once. Handing back one withdrawal per
    // claim puts the burden of remembering all of them on every caller, so a batch returns one.
    describe('registering a batch', () => {
        it('should claim every combination in the array', () => {
            const search = handlerMock();
            const dismiss = handlerMock();
            const toggle = handlerMock();
            register([
                { combination: 'mod+k', label: 'search', handler: search },
                { combination: 'escape', label: 'dismiss', handler: dismiss },
                { combination: 'mod+b', label: 'toggle', handler: toggle }
            ]);

            pressModK();
            press({ key: 'Escape' });
            press({ key: 'b', metaKey: true });

            expect(search).toHaveBeenCalledTimes(1);
            expect(dismiss).toHaveBeenCalledTimes(1);
            expect(toggle).toHaveBeenCalledTimes(1);
        });

        it('should withdraw the whole batch with a single call', () => {
            const search = handlerMock();
            const dismiss = handlerMock();
            const unregister = register([
                { combination: 'mod+k', label: 'search', handler: search },
                { combination: 'escape', label: 'dismiss', handler: dismiss }
            ]);

            unregister();
            pressModK();
            press({ key: 'Escape' });

            expect(search).not.toHaveBeenCalled();
            expect(dismiss).not.toHaveBeenCalled();
        });

        it('should restore the claims a batch was shadowing when it is withdrawn', () => {
            const portletSearch = jest.fn();
            register({ combination: 'mod+k', label: 'portlet search', handler: portletSearch });

            const dialogSearch = handlerMock();
            const unregisterDialog = register([
                { combination: 'mod+k', label: 'dialog search', handler: dialogSearch }
            ]);

            unregisterDialog();
            pressModK();

            expect(portletSearch).toHaveBeenCalledTimes(1);
            expect(dialogSearch).not.toHaveBeenCalled();
        });

        it('should tolerate withdrawing a batch twice', () => {
            const handler = handlerMock();
            const unregister = register([
                { combination: 'mod+k', label: 'search', handler },
                { combination: 'escape', label: 'dismiss', handler }
            ]);

            unregister();
            unregister();

            expect(() => pressModK()).not.toThrow();
        });

        it('should accept a single shortcut without an array', () => {
            const handler = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler });

            pressModK();

            expect(handler).toHaveBeenCalledTimes(1);
        });

        // Labels are what the documentation is generated from, and unlike combinations they have no
        // resolution rule: two claims sharing one just produce an ambiguous entry. In a single call
        // it is almost always a registration that was copied and only half edited.
        it('should reject the same label used twice in one call', () => {
            expect(() =>
                register([
                    { combination: 'mod+k', label: 'search', handler: handlerMock() },
                    { combination: 'escape', label: 'search', handler: handlerMock() }
                ])
            ).toThrow(/search/);
        });

        it('should register nothing at all when a batch is rejected', () => {
            const handler = handlerMock();

            expect(() =>
                register([
                    { combination: 'escape', label: 'dismiss', handler },
                    { combination: 'mod+b', label: 'dismiss', handler }
                ])
            ).toThrow();

            press({ key: 'Escape' });

            expect(handler).not.toHaveBeenCalled();
            expect(service.activeShortcuts()).toEqual([]);
        });

        // The model's own rule, which the label check must not break: a combination may be claimed
        // more than once and the most recent claim wins.
        it('should still allow the same combination twice in one call', () => {
            const first = handlerMock();
            const second = handlerMock();

            expect(() =>
                register([
                    { combination: 'mod+k', label: 'first', handler: first },
                    { combination: 'mod+k', label: 'second', handler: second }
                ])
            ).not.toThrow();

            pressModK();

            expect(second).toHaveBeenCalledTimes(1);
            expect(first).not.toHaveBeenCalled();
        });

        it('should allow separate calls to reuse a label, since only a batch is checked', () => {
            expect(() => {
                register({ combination: 'mod+k', label: 'search', handler: handlerMock() });
                register({ combination: 'escape', label: 'search', handler: handlerMock() });
            }).not.toThrow();
        });

        it('should leave an empty batch harmless', () => {
            const unregister = register([]);

            expect(() => unregister()).not.toThrow();
            expect(service.activeShortcuts()).toEqual([]);
        });
    });

    describe('declining', () => {
        it('should fall through to the previous claimant when the newest declines', () => {
            const first = handlerMock();
            const second = jest.fn(() => false);
            register({ combination: 'mod+k', label: 'search', handler: first });
            register({ combination: 'mod+k', label: 'search', handler: second });

            pressModK();

            expect(second).toHaveBeenCalledTimes(1);
            expect(first).toHaveBeenCalledTimes(1);
        });

        it('should leave the browser default intact when every claimant declines', () => {
            register({
                combination: 'mod+k',
                label: 'search',
                handler: () => false
            });

            const event = pressModK();

            expect(event.defaultPrevented).toBe(false);
        });
    });

    describe('browser defaults', () => {
        it('should suppress the browser default for a claimed combination', () => {
            register({ combination: 'mod+k', label: 'search', handler: handlerMock() });

            const event = pressModK();

            expect(event.defaultPrevented).toBe(true);
        });

        it('should never suppress a combination nothing claims', () => {
            register({ combination: 'mod+k', label: 'search', handler: handlerMock() });

            const event = press({ key: 'p', metaKey: true });

            expect(event.defaultPrevented).toBe(false);
        });

        // A component that handled the key on its own element is closer to the target and has already
        // marked the event. The registry must not act on it a second time.
        it('should ignore an event a nested handler already consumed', () => {
            const handler = handlerMock();
            register({ combination: 'mod+b', label: 'toggle', handler });

            const event = new KeyboardEvent('keydown', {
                key: 'b',
                metaKey: true,
                bubbles: true,
                cancelable: true
            });
            event.preventDefault();
            document.dispatchEvent(event);

            expect(handler).not.toHaveBeenCalled();
        });
    });

    /**
     * A bare printable key is the user typing whenever focus sits in something that takes text.
     *
     * This is what makes a single-character combination usable as a shortcut at all: without the
     * rule, typing `/` into the very search box the shortcut focuses would re-trigger the shortcut
     * instead of entering a character. The rule is deliberately narrow — a single-character `key`
     * with no modifier — so control keys and modifier combinations keep working while typing.
     */
    describe('typing in an editable target', () => {
        const mounted: HTMLElement[] = [];

        const mount = <T extends HTMLElement>(element: T): T => {
            document.body.appendChild(element);
            mounted.push(element);

            return element;
        };

        const pressFrom = (element: HTMLElement, init: KeyboardEventInit): KeyboardEvent => {
            const event = new KeyboardEvent('keydown', {
                bubbles: true,
                cancelable: true,
                ...init
            });
            element.dispatchEvent(event);

            return event;
        };

        const contentEditable = (): HTMLElement => {
            const surface = document.createElement('div');
            surface.setAttribute('contenteditable', 'true');
            surface.appendChild(document.createElement('p'));

            return surface;
        };

        afterEach(() => {
            mounted.splice(0).forEach((element) => element.remove());
        });

        it('should not fire a bare printable key from a text input', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            pressFrom(mount(document.createElement('input')), { key: '/' });

            expect(handler).not.toHaveBeenCalled();
        });

        it('should not fire a bare printable key from a textarea', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            pressFrom(mount(document.createElement('textarea')), { key: '/' });

            expect(handler).not.toHaveBeenCalled();
        });

        // Nested, because a rich text surface puts the caret inside a child node rather than on the
        // editable host itself.
        it('should not fire a bare printable key from inside a rich text surface', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            const surface = mount(contentEditable());
            pressFrom(surface.querySelector('p') as HTMLElement, { key: '/' });

            expect(handler).not.toHaveBeenCalled();
        });

        it('should leave the browser default alone for a key it suppressed', () => {
            register({ combination: '/', label: 'search', handler: handlerMock() });

            const event = pressFrom(mount(document.createElement('input')), { key: '/' });

            expect(event.defaultPrevented).toBe(false);
        });

        /**
         * An `input` is not automatically a text field. A checkbox takes no characters, so a bare
         * printable key there is a shortcut, not typing.
         *
         * This matters directly in the listing: rows carry checkboxes and they are the primary way
         * to select, so "tick a few rows, then press the search key" is an ordinary sequence. With
         * every `INPUT` treated as typing it did nothing at all.
         */
        it('should fire a bare printable key from a checkbox', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            pressFrom(mount(checkbox), { key: '/' });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it('should fire a bare printable key from a radio button', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            const radio = document.createElement('input');
            radio.type = 'radio';
            pressFrom(mount(radio), { key: '/' });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        // A typed input is still typing, whatever its type attribute says beyond the non-text set.
        it('should not fire from a search-typed input', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            const field = document.createElement('input');
            field.type = 'search';
            pressFrom(mount(field), { key: '/' });

            expect(handler).not.toHaveBeenCalled();
        });

        // Browsers use a printable key for type-ahead selection inside a `select`, so it is typing.
        it('should not fire from a select', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            pressFrom(mount(document.createElement('select')), { key: '/' });

            expect(handler).not.toHaveBeenCalled();
        });

        /**
         * The typing rule and the layout rule have to agree about Alt, or they cancel out.
         *
         * The layout rule treats Alt as something that can produce a character, so an Alt-typed `/`
         * matches a bare `/` claim. If the typing rule did not treat it the same way, that character
         * would be stolen out of a text field: not typing, therefore a shortcut. Both now ask the
         * same question — did this produce a character, with no Command or Control held.
         */
        it('should not fire an alt-produced character from a text input', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            pressFrom(mount(document.createElement('input')), { key: '/', altKey: true });

            expect(handler).not.toHaveBeenCalled();
        });

        it('should let an alt-produced character reach the field as text', () => {
            register({ combination: '/', label: 'search', handler: handlerMock() });

            const event = pressFrom(mount(document.createElement('input')), {
                key: '/',
                altKey: true
            });

            expect(event.defaultPrevented).toBe(false);
        });

        it('should fire a bare printable key from a non-editable element', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            pressFrom(mount(document.createElement('div')), { key: '/' });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        // The modifier form has to keep working from inside the box it focuses, or pressing it a
        // second time would be a dead key.
        it('should fire a modifier combination while typing', () => {
            const handler = handlerMock();
            register({ combination: 'mod+k', label: 'search', handler });

            pressFrom(mount(document.createElement('input')), { key: 'k', metaKey: true });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        // Escape produces no text, so it is not typing and must still reach its claimant.
        it('should fire a control key while typing', () => {
            const handler = handlerMock();
            register({ combination: 'escape', label: 'close', handler });

            pressFrom(mount(document.createElement('input')), { key: 'Escape' });

            expect(handler).toHaveBeenCalledTimes(1);
        });
    });

    /**
     * A printable character can need a modifier to type at all, and which modifier depends on the
     * keyboard layout: `/` is its own key on US QWERTY, Shift+7 on German QWERTZ and Spanish, and
     * Shift+: on French AZERTY. The browser reports the character that was produced *and* the
     * modifier that produced it, so folding that modifier into the lookup makes the same character
     * resolve differently per layout — and a `/` claim registered on a US machine is simply dead for
     * everyone else.
     *
     * Command and Control are never layout mechanics, so they are still required to match.
     */
    /**
     * `normalize()` is what keys the claim map, so if two spellings of the same combination stopped
     * resolving to one string the claims would silently split into separate stacks and last-in-wins
     * would stop working — with nothing else here to catch it. This registry is shared
     * infrastructure other surfaces register against, so the invariant is worth pinning directly.
     */
    /**
     * This is the single root service arbitrating every shortcut in the application, so one
     * consumer's bug must not take the key down for everyone. Left to propagate, a throwing handler
     * skipped `preventDefault`, denied every claim stacked beneath it a turn, and logged nothing —
     * the user saw a dead key with no diagnostic anywhere.
     *
     * A throw is treated as a decline: the handler did not successfully handle the key, so the next
     * claimant gets it. The error is still surfaced, so the bug does not go quiet.
     */
    describe('a handler that throws', () => {
        const boom = () => {
            throw new Error('handler exploded');
        };

        it('should pass the key to the claim underneath', () => {
            const underneath = handlerMock();
            register({ combination: 'mod+k', label: 'underneath', handler: underneath });
            register({ combination: 'mod+k', label: 'broken', handler: boom });

            pressModK();

            expect(underneath).toHaveBeenCalledTimes(1);
        });

        it('should report the failure with the shortcut that caused it', () => {
            const reported = jest.spyOn(console, 'error').mockImplementation(() => undefined);
            register({ combination: 'mod+k', label: 'broken', handler: boom });

            pressModK();

            expect(reported).toHaveBeenCalledWith(
                expect.stringContaining('broken'),
                expect.any(Error)
            );
            reported.mockRestore();
        });

        it('should leave the browser default alone when every claimant throws', () => {
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            register({ combination: 'mod+k', label: 'broken', handler: boom });

            const event = pressModK();

            expect(event.defaultPrevented).toBe(false);
        });

        it('should not break the next press', () => {
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            const withdraw = service.register({
                combination: 'mod+k',
                label: 'broken',
                handler: boom
            });
            pressModK();
            withdraw();

            const healthy = handlerMock();
            register({ combination: 'mod+k', label: 'healthy', handler: healthy });
            pressModK();

            expect(healthy).toHaveBeenCalledTimes(1);
        });
    });

    describe('combination normalisation', () => {
        it('should treat modifier orderings as one claim', () => {
            const first = handlerMock();
            const second = handlerMock();
            register({ combination: 'shift+mod+k', label: 'first', handler: first });
            register({ combination: 'mod+shift+k', label: 'second', handler: second });

            press({ key: 'k', metaKey: true, shiftKey: true });

            // One stack, so the newer claim shadows the older rather than both being live.
            expect(second).toHaveBeenCalledTimes(1);
            expect(first).not.toHaveBeenCalled();
        });

        it('should hand the combination back to the first ordering when the second withdraws', () => {
            const first = handlerMock();
            const second = handlerMock();
            register({ combination: 'shift+mod+k', label: 'first', handler: first });
            const withdraw = service.register({
                combination: 'mod+shift+k',
                label: 'second',
                handler: second
            });

            withdraw();
            press({ key: 'k', metaKey: true, shiftKey: true });

            expect(first).toHaveBeenCalledTimes(1);
        });

        it('should ignore surrounding whitespace and case', () => {
            const handler = handlerMock();
            register({ combination: '  MOD + Shift + K  ', label: 'spaced', handler });

            press({ key: 'k', metaKey: true, shiftKey: true });

            expect(handler).toHaveBeenCalledTimes(1);
        });
    });

    describe('keyboard layouts', () => {
        it('should fire a bare character claim when the layout needs shift to type it', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            // What a German QWERTZ Shift+7 looks like to the browser.
            press({ key: '/', shiftKey: true });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it('should fire a bare character claim when the layout needs alt to type it', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            press({ key: '/', altKey: true });

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it('should suppress the browser default for a shifted character press', () => {
            register({ combination: '/', label: 'search', handler: handlerMock() });

            const event = press({ key: '/', shiftKey: true });

            expect(event.defaultPrevented).toBe(true);
        });

        // Command and Control are real command modifiers, so they must not fall through to the bare
        // character. `Mod + /` is a different shortcut from `/`, on every layout.
        it('should not fire a bare character claim when a command modifier is held', () => {
            const handler = handlerMock();
            register({ combination: '/', label: 'search', handler });

            press({ key: '/', metaKey: true });

            expect(handler).not.toHaveBeenCalled();
        });

        // The more specific claim still wins where one exists, so this stays expressible.
        it('should prefer an explicit shift claim over the bare character', () => {
            const bare = handlerMock();
            const shifted = handlerMock();
            register({ combination: '/', label: 'search', handler: bare });
            register({ combination: 'shift+/', label: 'shifted search', handler: shifted });

            press({ key: '/', shiftKey: true });

            expect(shifted).toHaveBeenCalledTimes(1);
            expect(bare).not.toHaveBeenCalled();
        });

        // A multi-character key name is never produced by a layout, so it keeps matching exactly.
        it('should still require the modifier for a named key', () => {
            const handler = handlerMock();
            register({ combination: 'escape', label: 'close', handler });

            press({ key: 'Escape', shiftKey: true });

            expect(handler).not.toHaveBeenCalled();
        });
    });

    describe('documentation surface', () => {
        it('should expose the active shortcuts with their labels', () => {
            register({ combination: 'mod+k', label: 'search', handler: handlerMock() });
            register({ combination: 'escape', label: 'dismiss', handler: handlerMock() });

            expect(service.activeShortcuts()).toEqual([
                { combination: 'mod+k', label: 'search' },
                { combination: 'escape', label: 'dismiss' }
            ]);
        });

        it('should report only the claim that would actually receive the key', () => {
            register({ combination: 'mod+k', label: 'portlet search', handler: handlerMock() });
            register({ combination: 'mod+k', label: 'dialog search', handler: handlerMock() });

            expect(service.activeShortcuts()).toEqual([
                { combination: 'mod+k', label: 'dialog search' }
            ]);
        });
    });
});
