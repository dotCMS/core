import { DotContentState } from '@dotcms/dotcms-models';

import { DotContentletStatusPipe } from './dot-contentlet-status.pipe';

describe('DotContentletStatusPipe', () => {
    let pipe: DotContentletStatusPipe;

    beforeEach(() => {
        pipe = new DotContentletStatusPipe();
    });

    it('should return "Archived" when contentlet is archived', () => {
        const contentlet: DotContentState = {
            archived: true,
            live: false,
            working: false,
            hasLiveVersion: false
        };
        expect(pipe.transform(contentlet)).toEqual('Archived');
    });

    it('should return "Archived" when contentlet is deleted', () => {
        const contentlet: DotContentState = {
            deleted: true,
            live: false,
            working: false,
            hasLiveVersion: false
        };
        expect(pipe.transform(contentlet)).toEqual('Archived');
    });

    it('should return "Archived" when contentlet is both deleted and archived', () => {
        const contentlet: DotContentState = {
            deleted: true,
            archived: true,
            live: false,
            working: false,
            hasLiveVersion: false
        };
        expect(pipe.transform(contentlet)).toEqual('Archived');
    });

    it('should return "Published" when contentlet is live, has live version, and is working', () => {
        const contentlet: DotContentState = {
            live: true,
            working: true,
            hasLiveVersion: true,
            archived: false,
            deleted: false
        };
        expect(pipe.transform(contentlet)).toEqual('Published');
    });

    it('should return "Draft" when contentlet is live but missing hasLiveVersion', () => {
        const contentlet: DotContentState = {
            live: true,
            working: true,
            hasLiveVersion: false,
            archived: false,
            deleted: false
        };
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });

    it('should return "Draft" when contentlet is live but not working', () => {
        const contentlet: DotContentState = {
            live: true,
            working: false,
            hasLiveVersion: true,
            archived: false,
            deleted: false
        };
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });

    it('should return "Revision" when contentlet is not live but has live version', () => {
        const contentlet: DotContentState = {
            live: false,
            working: false,
            hasLiveVersion: true,
            archived: false,
            deleted: false
        };
        expect(pipe.transform(contentlet)).toEqual('Revision');
    });

    it('should return "Draft" when contentlet is not live and has no live version', () => {
        const contentlet: DotContentState = {
            live: false,
            working: false,
            hasLiveVersion: false,
            archived: false,
            deleted: false
        };
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });

    it('should return "Draft" when contentlet is null', () => {
        const contentlet = null as DotContentState;
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });

    it('should return "Draft" when contentlet is undefined', () => {
        const contentlet = undefined as DotContentState;
        expect(pipe.transform(contentlet)).toEqual('Draft');
    });
});
