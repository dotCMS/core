import {
    attachOrderedFields,
    ORDERED_FIELDS,
    orderedKeyValueText,
    parseOrderedKeyValue,
    parsePreservingKeyOrder
} from './key-value-order.util';

/** Mirrors what the service does: two parses of the same raw text. */
const recover = <T extends Record<string, unknown>>(raw: string): T => {
    const parsed = JSON.parse(raw) as { entity: T };
    const ordered = parsePreservingKeyOrder(raw) as { entity: unknown };

    return attachOrderedFields(parsed.entity, ordered.entity);
};

/** What the Key/Value resolver does for one field. */
const field = (raw: string, variable = 'keyValue') => orderedKeyValueText(recover(raw), variable);

describe('key/value order recovery', () => {
    it('should keep an integer-like key where the response put it', () => {
        const raw = '{"entity":{"keyValue":{"orden 1":"a","orden 2":"b","123":"c","zzz":"d"}}}';

        // Proof the problem is real: a plain parse hoists `123` to the front.
        expect(Object.keys(JSON.parse(raw).entity.keyValue)).toEqual([
            '123',
            'orden 1',
            'orden 2',
            'zzz'
        ]);

        // The resolver hands the form JSON text, which is what the control sends back
        // untouched if the user never edits it.
        expect(typeof field(raw)).toBe('string');
        expect(parseOrderedKeyValue(field(raw) as string)).toEqual([
            { key: 'orden 1', value: 'a' },
            { key: 'orden 2', value: 'b' },
            { key: '123', value: 'c' },
            { key: 'zzz', value: 'd' }
        ]);
    });

    it('should keep several integer-like keys in their own positions', () => {
        const raw = '{"entity":{"keyValue":{"200":"segundo","zzz":"medio","10":"ultimo"}}}';

        expect(parseOrderedKeyValue(field(raw) as string)).toEqual([
            { key: '200', value: 'segundo' },
            { key: 'zzz', value: 'medio' },
            { key: '10', value: 'ultimo' }
        ]);
    });

    it('should render a stored null as the literal "null" rather than dropping the pair', () => {
        const raw = '{"entity":{"keyValue":{"imported":null}}}';

        expect(parseOrderedKeyValue(field(raw) as string)).toEqual([
            { key: 'imported', value: 'null' }
        ]);
    });

    it('should handle an empty field', () => {
        expect(parseOrderedKeyValue(field('{"entity":{"keyValue":{}}}') as string)).toEqual([]);
    });

    it('should hand the form a value the backend accepts unchanged', () => {
        // The regression this guards: an array here reaches the form control, and a
        // contentlet opened and saved without editing sends it as-is. The backend
        // takes a JSON string or a map for these fields, never a list, and answers
        // "Invalid JSON field provided".
        const raw = '{"entity":{"keyValue":{"a":"1","2":"b"}}}';
        const value = field(raw);

        expect(typeof value).toBe('string');
        expect(() => JSON.parse(value as string)).not.toThrow();
    });

    describe('what it must not touch', () => {
        it("should leave a binary field's metadata a real object", () => {
            /*
             * The regression this guards, and the reason identification moved to the
             * content type: an earlier version picked out Key/Value fields by shape —
             * "a plain object whose values are all primitives" — and rewrote them into
             * JSON text. A binary field's `metaData` is exactly that shape, so it became
             * a string, `contentlet.metaData.name` became undefined, and the file
             * preview came back empty after a reload.
             */
            const raw = `{"entity":{
                "metaData":{"name":"file.txt","isImage":false,"fileSize":42,"sha256":"abc"},
                "fileAssetMetaData":{"name":"file.txt","length":42,"title":"file"}
            }}`;
            const result = recover(raw);

            expect(result.metaData).toEqual({
                name: 'file.txt',
                isImage: false,
                fileSize: 42,
                sha256: 'abc'
            });
            expect((result.metaData as Record<string, unknown>).name).toBe('file.txt');
            expect(typeof result.fileAssetMetaData).toBe('object');
        });

        it('should add nothing to a contentlet beyond its own namespace', () => {
            const raw = '{"entity":{"title":"a","keyValue":{"a":"1"}}}';
            const result = recover(raw);

            expect(Object.keys(result).filter((k) => k !== ORDERED_FIELDS)).toEqual([
                'title',
                'keyValue'
            ]);
        });

        it('should leave primitives, arrays and nested objects alone', () => {
            const raw = `{"entity":{
                "title":"a title",
                "languageId":1,
                "live":true,
                "categories":["inode-1","inode-2"],
                "binaryField":{"versionPath":"/x","metaData":{"width":10}}
            }}`;
            const result = recover(raw);

            expect(result.title).toBe('a title');
            expect(result.languageId).toBe(1);
            expect(result.live).toBe(true);
            expect(result.categories).toEqual(['inode-1', 'inode-2']);
            // Nested structure means it is not a Key/Value field.
            expect(result.binaryField).toEqual({
                versionPath: '/x',
                metaData: { width: 10 }
            });
        });

        it('should not corrupt a value that contains a digits-and-colon pattern', () => {
            // Quotes inside a JSON string read as `\\"`, so the key pattern cannot
            // match here — this pins that assumption.
            const raw = '{"entity":{"keyValue":{"nota":"mirá \\"123\\": esto"}}}';

            expect(parseOrderedKeyValue(field(raw) as string)).toEqual([
                { key: 'nota', value: 'mirá "123": esto' }
            ]);
        });

        it('should recover each Key/Value field independently', () => {
            const raw = '{"entity":{"first":{"9":"a","b":"c"},"second":{"z":"1","2":"3"}}}';

            expect(parseOrderedKeyValue(field(raw, 'first') as string)).toEqual([
                { key: '9', value: 'a' },
                { key: 'b', value: 'c' }
            ]);
            expect(parseOrderedKeyValue(field(raw, 'second') as string)).toEqual([
                { key: 'z', value: '1' },
                { key: '2', value: '3' }
            ]);
        });
    });

    describe('degrading safely', () => {
        it('should return the contentlet untouched when the ordered parse is unusable', () => {
            const contentlet = { keyValue: { a: '1' } };

            expect(attachOrderedFields(contentlet, null)).toBe(contentlet);
            expect(attachOrderedFields(contentlet, 'not an object')).toBe(contentlet);
            expect(orderedKeyValueText(contentlet, 'keyValue')).toBeNull();
        });

        it('should decline the recovered order when the keys do not match', () => {
            // A mismatch means the recovery is not trustworthy for this field, so the
            // caller falls back to the plainly-parsed value.
            const contentlet = attachOrderedFields(
                { keyValue: { a: '1', b: '2' } },
                { keyValue: { a: '1' } }
            );

            expect(orderedKeyValueText(contentlet, 'keyValue')).toBeNull();
        });

        it('should lose only the ordering, never a pair, if a key collides with the prefix', () => {
            // The prefix opens with NUL, which cannot be typed into the editor, but an
            // API import could still produce such a key. Prefixing then makes it identical
            // to the internal form of `7`, so one would overwrite the other. The key-count
            // guard catches exactly that and falls back to the object: both pairs survive,
            // and only the recovered order is given up.
            const raw = '{"entity":{"keyValue":{"\\u0000__dotKeyValue__7":"suya","7":"nuestra"}}}';
            const result = recover(raw);

            expect(typeof result.keyValue).toBe('object');
            expect(result.keyValue).toEqual({
                '\u0000__dotKeyValue__7': 'suya',
                '7': 'nuestra'
            });
        });
    });
});
