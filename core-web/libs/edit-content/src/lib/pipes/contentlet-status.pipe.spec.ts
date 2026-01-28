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

    it('should transform contentlet status to "Published" with success severity', () => {
        const contentlet = { live: true, working: false, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Published');
        expect(result.severity).toBe('success');
        expect(result.classes).toBe('p-chip-success');
    });

    it('should transform contentlet status to "Draft" with secondary severity', () => {
        const contentlet = { live: false, working: true, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Draft');
        expect(result.severity).toBe('secondary');
        expect(result.classes).toBe('');
    });

    it('should transform contentlet status to "Changed" with warn severity', () => {
        const contentlet = {
            live: true,
            working: true,
            workingInode: '1',
            liveInode: '2',
            archived: false
        } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Changed');
        expect(result.severity).toBe('warn');
        expect(result.classes).toBe('p-chip-pink');
    });

    it('should transform contentlet status to "Archived" with contrast severity', () => {
        const contentlet = { live: false, working: false, archived: true } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Archived');
        expect(result.severity).toBe('contrast');
        expect(result.classes).toBe('p-chip-gray');
    });

    it('should transform contentlet status to "Published" when live and working inodes match', () => {
        const contentlet = { live: true, working: true, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('Published');
        expect(result.severity).toBe('success');
        expect(result.classes).toBe('p-chip-success');
    });

    it('should transform contentlet status to empty label and secondary severity for unknown', () => {
        const contentlet = { live: false, working: false, archived: false } as DotCMSContentlet;

        const result = pipe.transform(contentlet);

        expect(result.label).toBe('');
        expect(result.severity).toBe('secondary');
        expect(result.classes).toBe('');
    });

    it('should transform undefined contentlet to "New" status with info severity', () => {
        const result = pipe.transform(undefined);

        expect(result.label).toBe('New');
        expect(result.severity).toBe('info');
        expect(result.classes).toBe('p-chip-blue');
    });
});
