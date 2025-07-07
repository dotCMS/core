import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentletStatusPipe } from './dot-contentlet-status.pipe';

describe('DotContentletStatusPipe', () => {
    let pipe: DotContentletStatusPipe;

    beforeEach(() => {
        pipe = new DotContentletStatusPipe();
    });

    it('should return "Archived" when contentlet is archived', () => {
        const contentlet = { archived: true, live: false } as DotCMSContentlet;
        expect(pipe.transform(contentlet)).toEqual('Archived');
    });

    it('should return "Published" when contentlet is live', () => {
        const contentlet = { archived: false, live: true } as DotCMSContentlet;
        expect(pipe.transform(contentlet)).toEqual('Published');
    });

    it('should return "Draft" when contentlet is not live or archived', () => {
        const contentlet = { archived: false, live: false } as DotCMSContentlet;
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });

    it('should return "Draft" when contentlet is null', () => {
        const contentlet = null as DotCMSContentlet;
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });
});
