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

    it('should keep an empty value rather than dropping the pair', () => {
        expect(parseKeyValueBlock('EMPTY=')).toEqual([{ key: 'EMPTY', value: '' }]);
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

    it('should handle CRLF line endings', () => {
        expect(parseKeyValueBlock('A=1\r\nB=2')).toEqual([
            { key: 'A', value: '1' },
            { key: 'B', value: '2' }
        ]);
    });
});
