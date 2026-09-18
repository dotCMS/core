import { parseKeyValueBlock } from './dot-key-value-paste.util';

describe('parseKeyValueBlock', () => {
    it('should read one pair per line, in the order given', () => {
        const pairs = parseKeyValueBlock('SOME=TEST\nJEJE=JEJE\nFOO=BAR');

        expect(pairs).toEqual([
            { key: 'SOME', value: 'TEST' },
            { key: 'JEJE', value: 'JEJE' },
            { key: 'FOO', value: 'BAR' }
        ]);
    });

    it('should split on the first equals only', () => {
        // The value of a URL or a connection string is full of them.
        const pairs = parseKeyValueBlock('URL=https://x.com/?a=1&b=2');

        expect(pairs).toEqual([{ key: 'URL', value: 'https://x.com/?a=1&b=2' }]);
    });

    it('should ignore comments and blank lines', () => {
        const pairs = parseKeyValueBlock('# a comment\n\nA=1\n\n  # indented\nB=2');

        expect(pairs).toEqual([
            { key: 'A', value: '1' },
            { key: 'B', value: '2' }
        ]);
    });

    it('should drop an export prefix', () => {
        const pairs = parseKeyValueBlock('export TOKEN=abc\n  export  OTHER=def');

        expect(pairs).toEqual([
            { key: 'TOKEN', value: 'abc' },
            { key: 'OTHER', value: 'def' }
        ]);
    });

    it('should remove one matching pair of surrounding quotes', () => {
        const pairs = parseKeyValueBlock(`A="with spaces"\nB='single'\nC=bare`);

        expect(pairs).toEqual([
            { key: 'A', value: 'with spaces' },
            { key: 'B', value: 'single' },
            { key: 'C', value: 'bare' }
        ]);
    });

    it('should leave unbalanced or inner quotes alone', () => {
        // Stripping greedily here would corrupt the value rather than tidy it.
        const pairs = parseKeyValueBlock(`A="unclosed\nB=say "hi" there`);

        expect(pairs).toEqual([
            { key: 'A', value: '"unclosed' },
            { key: 'B', value: 'say "hi" there' }
        ]);
    });

    it('should trim surrounding whitespace from the value', () => {
        expect(parseKeyValueBlock('A=  spaced  ')).toEqual([{ key: 'A', value: 'spaced' }]);
    });

    it('should drop a pair whose value is blank', () => {
        // `KEY=` is legal in a `.env`, but a blank value is refused everywhere else in
        // the editor, and a paste is not a way around that.
        expect(parseKeyValueBlock('EMPTY=')).toEqual([]);
        expect(parseKeyValueBlock('EMPTY="   "')).toEqual([]);
    });

    it('should keep the pairs around a blank one', () => {
        expect(parseKeyValueBlock('A=1\nEMPTY=\nB=2')).toEqual([
            { key: 'A', value: '1' },
            { key: 'B', value: '2' }
        ]);
    });

    it('should drop lines that are not assignments', () => {
        const pairs = parseKeyValueBlock('A=1\njust some prose\nB=2');

        expect(pairs).toEqual([
            { key: 'A', value: '1' },
            { key: 'B', value: '2' }
        ]);
    });

    it('should not overwrite a key that is already in the list', () => {
        const pairs = parseKeyValueBlock('TAKEN=new\nFRESH=ok', { TAKEN: true });

        expect(pairs).toEqual([{ key: 'FRESH', value: 'ok' }]);
    });

    it('should keep the first of a key repeated within the paste', () => {
        const pairs = parseKeyValueBlock('DUP=first\nDUP=second');

        expect(pairs).toEqual([{ key: 'DUP', value: 'first' }]);
    });

    it('should return nothing for text that carries no assignment', () => {
        // This is how the caller tells a block paste from someone pasting a plain key.
        expect(parseKeyValueBlock('JustAKeyName')).toEqual([]);
        expect(parseKeyValueBlock('')).toEqual([]);
    });

    it('should keep digits-only keys where the text puts them', () => {
        // The same hazard the ordering fix addresses: these must not drift to the front.
        const pairs = parseKeyValueBlock('alpha=1\n8080=port\nbeta=2');

        expect(pairs.map(({ key }) => key)).toEqual(['alpha', '8080', 'beta']);
    });

    describe('a pasted JSON object', () => {
        it('should read one pair per property, in the order written', () => {
            const pairs = parseKeyValueBlock(
                '{"id": 1, "nombre": "Ana Perez", "activo": true}'
            );

            expect(pairs).toEqual([
                { key: 'id', value: '1' },
                { key: 'nombre', value: 'Ana Perez' },
                { key: 'activo', value: 'true' }
            ]);
        });

        it('should read a selection taken out of the middle of a file', () => {
            // No braces and a trailing comma: what selecting a few lines actually yields.
            const pairs = parseKeyValueBlock('"id": 1,\n"correo": "ana@example.com",');

            expect(pairs).toEqual([
                { key: 'id', value: '1' },
                { key: 'correo', value: 'ana@example.com' }
            ]);
        });

        it('should write numbers and booleans the way the JSON wrote them', () => {
            const pairs = parseKeyValueBlock('{"edad": 28, "ratio": 0.5, "activo": false}');

            expect(pairs).toEqual([
                { key: 'edad', value: '28' },
                { key: 'ratio', value: '0.5' },
                { key: 'activo', value: 'false' }
            ]);
        });

        it('should keep a nested object or array as its JSON text', () => {
            // Dropping it would lose data in silence; as text it stays visible and editable.
            const pairs = parseKeyValueBlock('{"meta": {"b": 1}, "tags": [1, 2]}');

            expect(pairs).toEqual([
                { key: 'meta', value: '{"b":1}' },
                { key: 'tags', value: '[1,2]' }
            ]);
        });

        it('should skip a null or blank value, as KEY= is skipped', () => {
            const pairs = parseKeyValueBlock('{"a": null, "b": "", "c": "   ", "d": "ok"}');

            expect(pairs).toEqual([{ key: 'd', value: 'ok' }]);
        });

        it('should skip a key already in the list rather than overwrite it', () => {
            const pairs = parseKeyValueBlock('{"a": "new", "b": "2"}', { a: true });

            expect(pairs).toEqual([{ key: 'b', value: '2' }]);
        });

        it('should ignore a blank key, which has nothing to show in the key column', () => {
            const pairs = parseKeyValueBlock('{"": "orphan", "  ": "also", "a": "1"}');

            expect(pairs).toEqual([{ key: 'a', value: '1' }]);
        });

        it('should trim whitespace around a key', () => {
            const pairs = parseKeyValueBlock('{" spaced ": "1"}');

            expect(pairs).toEqual([{ key: 'spaced', value: '1' }]);
        });

        it('should return nothing for JSON that is not an object', () => {
            // No keys in any of these to make pairs from.
            expect(parseKeyValueBlock('[1, 2, 3]')).toEqual([]);
            expect(parseKeyValueBlock('"just a string"')).toEqual([]);
            expect(parseKeyValueBlock('42')).toEqual([]);
        });

        it('should return nothing for a selection that cut a string in half', () => {
            // Malformed past repair: the caller lets the browser paste it so the user sees it.
            expect(parseKeyValueBlock('"correo": "ana.perez@example.com')).toEqual([]);
        });

        it('should still read an env block, which is not JSON', () => {
            // The JSON attempt must not swallow the shape this parser started with.
            expect(parseKeyValueBlock('A=1\nB=2')).toEqual([
                { key: 'A', value: '1' },
                { key: 'B', value: '2' }
            ]);
        });
    });

    it('should handle CRLF line endings', () => {
        expect(parseKeyValueBlock('A=1\r\nB=2')).toEqual([
            { key: 'A', value: '1' },
            { key: 'B', value: '2' }
        ]);
    });
});
