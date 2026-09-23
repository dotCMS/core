import { isCodeToolResult, toModelOutput, toolResultText } from './results';

describe('isCodeToolResult', () => {
    it('recognizes exactly one string `result` key', () => {
        expect(isCodeToolResult({ result: 'text' })).toBe(true);
    });

    it.each([
        ['a manifest that happens to have a result field', { result: 'x', path: '/a' }],
        ['a non-string result', { result: 42 }],
        ['a bare string', 'text'],
        ['null', null]
    ])('rejects %s', (_label, value) => {
        expect(isCodeToolResult(value)).toBe(false);
    });
});

describe('toolResultText', () => {
    it('passes code text through unescaped', () => {
        expect(toolResultText({ result: 'a\n"b"' })).toBe('a\n"b"');
    });

    it('pretty-prints anything else — a manifest, a failure', () => {
        const failure = { ok: false, code: 'HTTP', error: 'boom', retryable: true };

        expect(toolResultText(failure)).toBe(JSON.stringify(failure, null, 2));
    });
});

describe('toModelOutput', () => {
    it('sends code text as text and everything else as JSON', () => {
        expect(toModelOutput({ output: { result: 'text' } })).toEqual({
            type: 'text',
            value: 'text'
        });
        expect(toModelOutput({ output: { result: 'x', path: '/a' } })).toEqual({
            type: 'json',
            value: { result: 'x', path: '/a' }
        });
    });
});
