import { modelOutputText, renderToolOutput, type CodeToolResult } from './results';
import { toToolFailure } from './tool-runtime';

const asText = ({ result }: CodeToolResult) => result;

describe('renderToolOutput', () => {
    it('shows a text tool’s result as text', () => {
        expect(renderToolOutput(asText, { result: 'a\n"b"' })).toEqual({
            type: 'text',
            value: 'a\n"b"'
        });
    });

    it('shows a tool without a text form as JSON — whatever its result looks like', () => {
        // A result shaped exactly like a code tool's is still JSON when its tool declared no
        // text form: the choice belongs to the tool, not to the value's shape.
        expect(renderToolOutput(undefined, { result: 'looks like text' })).toEqual({
            type: 'json',
            value: { result: 'looks like text' }
        });
    });

    it('shows a JSON result as the plain data it serializes to', () => {
        // The AI SDK types a JSON output's value as JSONValue, and every transport sends it as
        // JSON. Normalizing here makes the value handed over exactly what the model receives.
        expect(renderToolOutput(undefined, { when: new Date(0), gone: undefined, n: 1 })).toEqual({
            type: 'json',
            value: { when: '1970-01-01T00:00:00.000Z', n: 1 }
        });
    });

    it('shows a failure as JSON even from a text tool', () => {
        const failure = toToolFailure('search', new Error('boom'));

        expect(renderToolOutput<CodeToolResult>(asText, failure)).toEqual({
            type: 'json',
            value: failure
        });
    });
});

describe('modelOutputText', () => {
    it('carries text as-is and pretty-prints JSON', () => {
        expect(modelOutputText({ type: 'text', value: 'a\nb' })).toBe('a\nb');
        expect(modelOutputText({ type: 'json', value: { path: '/a' } })).toBe(
            JSON.stringify({ path: '/a' }, null, 2)
        );
    });
});
