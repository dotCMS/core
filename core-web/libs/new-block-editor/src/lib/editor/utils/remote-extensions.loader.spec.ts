import { Extension } from '@tiptap/core';

import type { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { loadRemoteExtensions, parseCustomBlocksField } from './remote-extensions.loader';

/**
 * Coverage for the `customBlocks` field-variable parsing that gates remote-extension
 * loading (#36646). The editor must degrade gracefully — a missing, malformed, or
 * schema-mismatched value yields `{ extensions: [] }` (the legacy contract) rather than
 * throwing, and a valid value is returned verbatim so the slow path loads the modules.
 */
describe('parseCustomBlocksField', () => {
    const fieldWith = (value: string | undefined): DotCMSContentTypeField =>
        ({
            fieldVariables: value === undefined ? [] : [{ key: 'customBlocks', value }]
        }) as unknown as DotCMSContentTypeField;

    it('returns empty when the field is undefined', () => {
        expect(parseCustomBlocksField(undefined)).toEqual({ extensions: [] });
    });

    it('returns empty when the customBlocks variable is absent', () => {
        expect(parseCustomBlocksField(fieldWith(undefined))).toEqual({ extensions: [] });
    });

    it('returns empty and warns on invalid JSON', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

        expect(parseCustomBlocksField(fieldWith('{ not json'))).toEqual({ extensions: [] });
        expect(warn).toHaveBeenCalled();

        warn.mockRestore();
    });

    it('returns empty and warns when the shape does not match the schema', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

        // `extensions` present but an entry is missing the required `url`.
        expect(
            parseCustomBlocksField(fieldWith(JSON.stringify({ extensions: [{ foo: 'bar' }] })))
        ).toEqual({ extensions: [] });
        expect(warn).toHaveBeenCalled();

        warn.mockRestore();
    });

    it('returns the parsed payload for a valid customBlocks value', () => {
        const payload = {
            extensions: [
                {
                    url: 'https://example.com/ext.js',
                    actions: [
                        {
                            command: 'insertThing',
                            menuLabel: 'Thing',
                            icon: 'extension',
                            name: 'customThing'
                        }
                    ]
                }
            ]
        };

        expect(parseCustomBlocksField(fieldWith(JSON.stringify(payload)))).toEqual(payload);
    });

    it('accepts an extension entry without actions', () => {
        const payload = { extensions: [{ url: 'https://example.com/ext.js' }] };

        expect(parseCustomBlocksField(fieldWith(JSON.stringify(payload)))).toEqual(payload);
    });
});

describe('loadRemoteExtensions', () => {
    it('warns when a declared remote block name does not match a loaded extension', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const importer = jest.fn().mockResolvedValue({
            customGalleryExtension: Extension.create({ name: 'loadedGallery' })
        });

        await loadRemoteExtensions(
            {
                extensions: [
                    {
                        url: 'https://example.com/custom-gallery.js',
                        actions: [
                            {
                                command: 'insertGallery',
                                menuLabel: 'Custom Gallery',
                                icon: 'photo_library',
                                name: 'customGallery'
                            }
                        ]
                    }
                ]
            },
            importer
        );

        expect(importer).toHaveBeenCalledWith('https://example.com/custom-gallery.js');
        expect(warn).toHaveBeenCalledWith(
            '[remote-extension] declared action.name "customGallery" did not match any loaded node'
        );

        warn.mockRestore();
    });

    it('does not warn when a declared remote block name matches a loaded extension', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

        await loadRemoteExtensions(
            {
                extensions: [
                    {
                        url: 'https://example.com/custom-gallery.js',
                        actions: [
                            {
                                command: 'insertGallery',
                                menuLabel: 'Custom Gallery',
                                icon: 'photo_library',
                                name: 'customGallery'
                            }
                        ]
                    }
                ]
            },
            jest.fn().mockResolvedValue({
                customGalleryExtension: Extension.create({ name: 'customGallery' })
            })
        );

        expect(warn).not.toHaveBeenCalled();

        warn.mockRestore();
    });
});
