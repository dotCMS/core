import { DotIsoCodePipe } from './dot-iso-code.pipe';

describe('DotIsoCodePipe', () => {
    let pipe: DotIsoCodePipe;

    beforeEach(() => {
        pipe = new DotIsoCodePipe();
    });

    it('transforms "en-gb" to "en-GB"', () => {
        expect(pipe.transform('en-gb')).toBe('en-GB');
    });

    it('transforms "FR-fr" to "fr-FR"', () => {
        expect(pipe.transform('FR-fr')).toBe('fr-FR');
    });

    it('returns original value for invalid ISO code "invalid"', () => {
        expect(pipe.transform('invalid')).toBe('invalid');
    });

    it('returns empty string for null input', () => {
        expect(pipe.transform(null)).toBe('');
    });

    it('returns empty string for undefined input', () => {
        expect(pipe.transform(undefined)).toBe('');
    });

    it('returns original value for non-matching pattern "en"', () => {
        expect(pipe.transform('en')).toBe('en');
    });

    it('returns original value for non-matching pattern "en-GBR"', () => {
        expect(pipe.transform('en-GBR')).toBe('en-GBR');
    });

    it('transforms "Es-ES" to "es-ES"', () => {
        expect(pipe.transform('Es-ES')).toBe('es-ES');
    });
});
