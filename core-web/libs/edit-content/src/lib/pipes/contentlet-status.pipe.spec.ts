import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentletStatusPipe } from './contentlet-status.pipe';

describe('ContentletStatusPipe', () => {
    let pipe: ContentletStatusPipe;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ContentletStatusPipe,
                { provide: DotMessageService, useValue: { get: (arg) => arg } }
            ]
        });
        pipe = TestBed.inject(ContentletStatusPipe);
    });

    it('should transform contentlet status to "Published"', () => {
        const contentlet = { live: true, working: false, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Published');
        expect(result.classes).toBe('p-chip-success');
    });

    it('should transform contentlet status to "Draft"', () => {
        const contentlet = { live: false, working: true, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Draft');
        expect(result.classes).toBe('');
    });

    it('should transform contentlet status to "Archived"', () => {
        const contentlet = { live: false, working: false, archived: true } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Archived');
        expect(result.classes).toBe('p-chip-gray');
    });

    it('should transform contentlet status to "Published"', () => {
        const contentlet = { live: true, working: true, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Published');
        expect(result.classes).toBe('p-chip-success');
    });

    it('should transform contentlet status to empty label and default classes', () => {
        const contentlet = { live: false, working: false, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('');
        expect(result.classes).toBe('');
    });

    it('should transform undefined contentlet to "New" status', () => {
        const result = pipe.transform(undefined);

        expect(result.label).toBe('New');
        expect(result.classes).toBe('p-chip-blue');
    });
});
