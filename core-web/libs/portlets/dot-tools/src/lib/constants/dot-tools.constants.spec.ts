import {
    DOT_TOOLS_FALLBACK_ICON,
    DOT_TOOLS_SECTION_ICONS,
    resolveSectionIcon
} from './dot-tools.constants';

describe('resolveSectionIcon', () => {
    it('returns the icon unchanged when it is a known Material Icon id', () => {
        expect(resolveSectionIcon('settings')).toBe('settings');
        expect(resolveSectionIcon('widgets')).toBe('widgets');
        expect(resolveSectionIcon(DOT_TOOLS_SECTION_ICONS[0])).toBe(DOT_TOOLS_SECTION_ICONS[0]);
    });

    it('falls back for a legacy Font Awesome id Task210316 would have replaced', () => {
        expect(resolveSectionIcon('fa-home')).toBe(DOT_TOOLS_FALLBACK_ICON);
        expect(resolveSectionIcon('fa-cog')).toBe(DOT_TOOLS_FALLBACK_ICON);
    });

    it('falls back for an unknown Material-Icon-shaped string', () => {
        expect(resolveSectionIcon('this_glyph_does_not_exist')).toBe(DOT_TOOLS_FALLBACK_ICON);
    });

    it('falls back for empty / null / undefined', () => {
        expect(resolveSectionIcon('')).toBe(DOT_TOOLS_FALLBACK_ICON);
        expect(resolveSectionIcon(null)).toBe(DOT_TOOLS_FALLBACK_ICON);
        expect(resolveSectionIcon(undefined)).toBe(DOT_TOOLS_FALLBACK_ICON);
    });

    it('is case-sensitive — Material Icons are all lowercase, so uppercase misses', () => {
        // We do not auto-lowercase: a stored "Settings" is still invalid data,
        // falling back makes the broken state visible.
        expect(resolveSectionIcon('Settings')).toBe(DOT_TOOLS_FALLBACK_ICON);
    });
});
