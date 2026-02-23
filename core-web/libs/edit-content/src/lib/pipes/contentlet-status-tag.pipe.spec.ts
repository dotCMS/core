import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentletStatusTagPipe } from './contentlet-status-tag.pipe';

describe('ContentletStatusTagPipe', () => {
    let pipe: ContentletStatusTagPipe;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ContentletStatusTagPipe,
                { provide: DotMessageService, useValue: { get: (arg: string) => arg } }
            ]
        });
        pipe = TestBed.inject(ContentletStatusTagPipe);
    });

    it('should return Published with success severity', () => {
        const contentlet = { live: true, working: false, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result).toEqual({ label: 'Published', severity: 'success' });
    });

    it('should return Draft with secondary severity', () => {
        const contentlet = { live: false, working: true, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result).toEqual({ label: 'Draft', severity: 'secondary' });
    });

    it('should return Changed with warn severity', () => {
        const contentlet = {
            live: true,
            working: true,
            workingInode: '1',
            liveInode: '2',
            archived: false
        } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result).toEqual({ label: 'Changed', severity: 'warn' });
    });

    it('should return Archived with contrast severity', () => {
        const contentlet = { live: false, working: false, archived: true } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result).toEqual({ label: 'Archived', severity: 'contrast' });
    });

    it('should return null for unknown status', () => {
        const contentlet = { live: false, working: false, archived: false } as DotCMSContentlet;
        const result = pipe.transform(contentlet);
        expect(result).toBeNull();
    });

    it('should return New with info severity for undefined contentlet', () => {
        const result = pipe.transform(undefined);
        expect(result).toEqual({ label: 'New', severity: 'info' });
    });
});
