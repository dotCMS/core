import { DotHostDepthPipe, getHostDepth } from './dot-host-depth.pipe';

// ---------------------------------------------------------------------------
// Pure utility function – getHostDepth()
// ---------------------------------------------------------------------------
describe('getHostDepth (utility function)', () => {
    // -----------------------------------------------------------------------
    // Null / empty / undefined inputs
    // -----------------------------------------------------------------------
    describe('null / empty / undefined inputs', () => {
        it('should return 0 for null', () => {
            expect(getHostDepth(null)).toBe(0);
        });

        it('should return 0 for undefined', () => {
            expect(getHostDepth(undefined)).toBe(0);
        });

        it('should return 0 for an empty string', () => {
            expect(getHostDepth('')).toBe(0);
        });
    });

    // -----------------------------------------------------------------------
    // Root-level hosts (parentPath === "/")
    // -----------------------------------------------------------------------
    describe('root-level hosts – parentPath "/"', () => {
        it('should return 0 for a plain root path "/"', () => {
            expect(getHostDepth('/')).toBe(0);
        });

        it('should return 0 for a root path with trailing slash "//"', () => {
            expect(getHostDepth('//')).toBe(0);
        });

        it('should return 0 for multiple consecutive slashes "///"', () => {
            expect(getHostDepth('///')).toBe(0);
        });
    });

    // -----------------------------------------------------------------------
    // Depth-1 hosts (one segment)
    // -----------------------------------------------------------------------
    describe('depth-1 hosts – single path segment', () => {
        it('should return 1 for "/parent/"', () => {
            expect(getHostDepth('/parent/')).toBe(1);
        });

        it('should return 1 for "/parent" (no trailing slash)', () => {
            expect(getHostDepth('/parent')).toBe(1);
        });

        it('should return 1 for "parent/" (no leading slash)', () => {
            expect(getHostDepth('parent/')).toBe(1);
        });

        it('should return 1 for "parent" (no slashes at all)', () => {
            expect(getHostDepth('parent')).toBe(1);
        });

        it('should return 1 for "//parent//" (extra slashes)', () => {
            expect(getHostDepth('//parent//')).toBe(1);
        });
    });

    // -----------------------------------------------------------------------
    // Depth-2 hosts (two segments)
    // -----------------------------------------------------------------------
    describe('depth-2 hosts – two path segments', () => {
        it('should return 2 for "/parent/child/"', () => {
            expect(getHostDepth('/parent/child/')).toBe(2);
        });

        it('should return 2 for "/parent/child" (no trailing slash)', () => {
            expect(getHostDepth('/parent/child')).toBe(2);
        });

        it('should return 2 for "//parent//child//" (duplicate slashes)', () => {
            expect(getHostDepth('//parent//child//')).toBe(2);
        });
    });

    // -----------------------------------------------------------------------
    // Deeply nested hosts (three or more segments)
    // -----------------------------------------------------------------------
    describe('deeply nested hosts – three or more segments', () => {
        it('should return 3 for "/a/b/c/"', () => {
            expect(getHostDepth('/a/b/c/')).toBe(3);
        });

        it('should return 4 for "/a/b/c/d/"', () => {
            expect(getHostDepth('/a/b/c/d/')).toBe(4);
        });

        it('should return 5 for "/level1/level2/level3/level4/level5/"', () => {
            expect(getHostDepth('/level1/level2/level3/level4/level5/')).toBe(5);
        });

        it('should handle 10 segments correctly', () => {
            const path = '/a/b/c/d/e/f/g/h/i/j/';
            expect(getHostDepth(path)).toBe(10);
        });
    });

    // -----------------------------------------------------------------------
    // Real-world dotCMS-style paths
    // -----------------------------------------------------------------------
    describe('real-world dotCMS-style parentPath values', () => {
        it('should handle a typical nested-host parentPath "/acme.com/"', () => {
            expect(getHostDepth('/acme.com/')).toBe(1);
        });

        it('should handle "/acme.com/us.acme.com/" (region under brand host)', () => {
            expect(getHostDepth('/acme.com/us.acme.com/')).toBe(2);
        });

        it('should handle hostnames with dots in path segments', () => {
            // dotCMS uses hostname as asset_name which often contains dots
            expect(getHostDepth('/parent.example.com/')).toBe(1);
            expect(getHostDepth('/parent.example.com/child.example.com/')).toBe(2);
        });

        it('should handle paths with hyphens and underscores', () => {
            expect(getHostDepth('/my-brand/my_region/')).toBe(2);
        });

        it('should treat the System Host root path "/" as depth 0', () => {
            // System Host lives at "/" – other hosts parented to System are depth 0
            expect(getHostDepth('/')).toBe(0);
        });
    });
});

// ---------------------------------------------------------------------------
// Angular Pipe – DotHostDepthPipe
// ---------------------------------------------------------------------------
describe('DotHostDepthPipe', () => {
    let pipe: DotHostDepthPipe;

    beforeEach(() => {
        pipe = new DotHostDepthPipe();
    });

    describe('transform', () => {
        it('should be instantiated without errors', () => {
            expect(pipe).toBeTruthy();
        });

        it('should return 0 for null input', () => {
            expect(pipe.transform(null)).toBe(0);
        });

        it('should return 0 for undefined input', () => {
            expect(pipe.transform(undefined)).toBe(0);
        });

        it('should return 0 for empty string', () => {
            expect(pipe.transform('')).toBe(0);
        });

        it('should return 0 for root path "/"', () => {
            expect(pipe.transform('/')).toBe(0);
        });

        it('should return 1 for single-segment path "/parent/"', () => {
            expect(pipe.transform('/parent/')).toBe(1);
        });

        it('should return 2 for two-segment path "/parent/child/"', () => {
            expect(pipe.transform('/parent/child/')).toBe(2);
        });

        it('should return 3 for three-segment path "/a/b/c/"', () => {
            expect(pipe.transform('/a/b/c/')).toBe(3);
        });

        it('should delegate to getHostDepth – consistent results', () => {
            const paths = [
                null,
                undefined,
                '',
                '/',
                '/parent/',
                '/parent/child/',
                '/a/b/c/',
                '/acme.com/us.acme.com/'
            ] as Array<string | null | undefined>;

            paths.forEach((path) => {
                expect(pipe.transform(path)).toBe(getHostDepth(path));
            });
        });
    });
});
