import { vi } from 'vitest';

import { DOT_VELOCITY_LANGUAGE_ID, ensureDotVelocityLanguageRegistered } from './velocity-language';

describe('ensureDotVelocityLanguageRegistered', () => {
    let register: ReturnType<typeof vi.fn>;
    let setMonarchTokensProvider: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        register = vi.fn();
        setMonarchTokensProvider = vi.fn();
    });

    afterEach(() => {
        delete (globalThis as { monaco?: unknown }).monaco;
    });

    function loadMonaco(knownLanguages: string[] = []): void {
        (globalThis as { monaco?: unknown }).monaco = {
            languages: {
                getLanguages: () => knownLanguages.map((id) => ({ id })),
                register,
                setMonarchTokensProvider
            }
        };
    }

    it('registers Velocity for .vtl files with its grammar once Monaco is loaded', () => {
        loadMonaco();

        ensureDotVelocityLanguageRegistered();

        expect(register).toHaveBeenCalledWith({
            id: DOT_VELOCITY_LANGUAGE_ID,
            extensions: ['.vtl'],
            mimetypes: ['text/x-velocity']
        });
        expect(setMonarchTokensProvider).toHaveBeenCalledWith(
            DOT_VELOCITY_LANGUAGE_ID,
            expect.objectContaining({ tokenizer: expect.any(Object) })
        );
    });

    it('does nothing before Monaco has loaded', () => {
        expect(() => ensureDotVelocityLanguageRegistered()).not.toThrow();
        expect(register).not.toHaveBeenCalled();
    });

    it('does not register the language a second time', () => {
        loadMonaco([DOT_VELOCITY_LANGUAGE_ID]);

        ensureDotVelocityLanguageRegistered();

        expect(register).not.toHaveBeenCalled();
        expect(setMonarchTokensProvider).not.toHaveBeenCalled();
    });

    // Monaco only reports a rule pointing at a state that does not exist when it tokenizes that
    // rule, so a typo in a state name would surface as an error in some editor at runtime.
    it('only moves to and includes states the grammar defines', () => {
        loadMonaco();
        ensureDotVelocityLanguageRegistered();

        const grammar = setMonarchTokensProvider.mock.calls[0][1] as {
            tokenizer: Record<string, unknown[]>;
        };
        const states = new Set(Object.keys(grammar.tokenizer));
        const builtIns = new Set(['pop', 'push', 'popall']);
        const referenced = new Set<string>();

        // `@scriptEmbedded.$S2` falls back to `scriptEmbedded`, so the part before the dot counts.
        const collect = (target: unknown) => {
            if (typeof target === 'string' && target.startsWith('@')) {
                referenced.add(target.slice(1).split('.')[0]);
            }
        };

        const visit = (action: unknown) => {
            if (Array.isArray(action)) {
                action.forEach(visit);
            } else if (action && typeof action === 'object') {
                const { next, switchTo, include } = action as Record<string, unknown>;
                [next, switchTo, include].forEach(collect);
            }
        };

        Object.values(grammar.tokenizer).forEach((rules) =>
            rules.forEach((rule) => {
                if (Array.isArray(rule)) {
                    visit(rule[1]);
                    collect(rule[2]);
                } else {
                    visit(rule);
                }
            })
        );

        const missing = [...referenced].filter((name) => !states.has(name) && !builtIns.has(name));

        expect(missing).toEqual([]);
    });
});
