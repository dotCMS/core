import { BuilderField, BuilderSection, getDuplicateIdentifiers } from './models';

function makeField(uid: string, identifier: string): BuilderField {
    return {
        uid,
        type: 'input',
        label: 'Label',
        identifier,
        inputType: 'text',
        placeholder: '',
        columns: 1,
        options: []
    };
}

function makeSection(uid: string, ...identifiers: string[]): BuilderSection {
    return {
        uid,
        title: 'Section',
        fields: identifiers.map((id, i) => makeField(`${uid}-f${i}`, id))
    };
}

describe('getDuplicateIdentifiers', () => {
    it('returns an empty Set when every field in a single section has a unique identifier', () => {
        const sections = [makeSection('s1', 'fontSize', 'fontWeight', 'lineHeight')];
        expect(getDuplicateIdentifiers(sections).size).toBe(0);
    });

    it('returns an empty Set when every field across multiple sections has a unique identifier', () => {
        const sections = [
            makeSection('s1', 'fontSize', 'fontWeight'),
            makeSection('s2', 'lineHeight', 'color')
        ];
        expect(getDuplicateIdentifiers(sections).size).toBe(0);
    });

    it('returns the shared identifier when two fields in the same section use it', () => {
        const sections = [makeSection('s1', 'fontSize', 'fontWeight', 'fontSize')];
        expect(getDuplicateIdentifiers(sections)).toEqual(new Set(['fontSize']));
    });

    it('returns the shared identifier when two fields in different sections use it', () => {
        const sections = [makeSection('s1', 'fontSize'), makeSection('s2', 'fontSize')];
        expect(getDuplicateIdentifiers(sections)).toEqual(new Set(['fontSize']));
    });

    it('ignores blank identifiers and does not flag them as duplicates', () => {
        const sections = [makeSection('s1', '', '')];
        expect(getDuplicateIdentifiers(sections).size).toBe(0);
    });

    it('returns all conflicting identifiers when multiple independent identifier pairs clash', () => {
        const sections = [
            makeSection('s1', 'fontSize', 'fontWeight'),
            makeSection('s2', 'fontSize', 'fontWeight')
        ];
        expect(getDuplicateIdentifiers(sections)).toEqual(new Set(['fontSize', 'fontWeight']));
    });
});
