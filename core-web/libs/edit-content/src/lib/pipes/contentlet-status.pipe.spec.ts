import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentletStatusPipe } from './contentlet-status.pipe';

describe('ContentletStatusPipe', () => {
    let pipe: ContentletStatusPipe;

    beforeEach(() => {
        pipe = new ContentletStatusPipe();
    });

    it('should create an instance', () => {
        expect(pipe).toBeTruthy();
    });

    it('should return the correct label and classes for a published contentlet', () => {
        const contentlet = { live: true, working: false, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result.label).toBe('Published');
        expect(result.classes).toBe('p-chip-success p-chip-sm');
    });

    it('should return the correct label and classes for a draft contentlet', () => {
        const contentlet = { live: false, working: true, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result.label).toBe('Draft');
        expect(result.classes).toBe('p-chip-sm');
    });

    it('should return the correct label and classes for an archived contentlet', () => {
        const contentlet = { live: false, working: false, archived: true } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result.label).toBe('Archived');
        expect(result.classes).toBe('p-chip-gray p-chip-sm');
    });

    it('should return the correct label and classes for a contentlet in revision', () => {
        const contentlet = { live: true, working: true, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result.label).toBe('Revision');
        expect(result.classes).toBe('p-chip-pink p-chip-sm');
    });

    it('should return the default label and classes for an unknown contentlet', () => {
        const contentlet = { live: false, working: false, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result.label).toBe('');
        expect(result.classes).toBe('p-chip-sm');
    });
});
