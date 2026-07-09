import { provideLocationMocks } from '@angular/common/testing';
import { Component } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { DotRelatedContentNavigationStore } from './dot-related-content-navigation.store';

@Component({ selector: 'dot-test-stub', template: 'stub', standalone: true })
class StubCmp {}

const A = { inode: 'inode-a', title: 'Content A' };
const B = { inode: 'inode-b', title: 'Content B' };
const C = { inode: 'inode-c', title: 'Content C' };

describe('DotRelatedContentNavigationStore', () => {
    let store: InstanceType<typeof DotRelatedContentNavigationStore>;
    let router: Router;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideRouter([{ path: 'content/:id', component: StubCmp }]),
                provideLocationMocks()
            ]
        });
        router = TestBed.inject(Router);
        store = TestBed.inject(DotRelatedContentNavigationStore);
    });

    describe('URL-driven trail (full-screen)', () => {
        it('starts with an empty trail', fakeAsync(() => {
            router.navigateByUrl('/content/inode-a');
            tick();

            expect(store.trailInodes()).toEqual([]);
            expect(store.trail()).toEqual([]);
        }));

        it('derives the trail from the rc query param', fakeAsync(() => {
            router.navigateByUrl('/content/inode-c?rc=inode-a,inode-b,inode-c');
            tick();

            expect(store.trailInodes()).toEqual([A.inode, B.inode, C.inode]);
        }));

        it('shows a placeholder for a crumb whose title is not cached (cold load)', fakeAsync(() => {
            router.navigateByUrl('/content/inode-c?rc=inode-a,inode-b,inode-c');
            tick();

            const trail = store.trail();
            expect(trail.map((c) => c.inode)).toEqual([A.inode, B.inode, C.inode]);
            expect(trail.every((c) => c.title === '…')).toBe(true);
        }));
    });

    describe('appendToTrail', () => {
        it('seeds [current, target] when starting fresh and registers titles', fakeAsync(() => {
            router.navigateByUrl('/content/inode-a');
            tick();

            const next = store.appendToTrail(A, B);
            expect(next).toEqual([A.inode, B.inode]);

            // Titles were registered, so the crumbs are labeled once the trail
            // reflects them.
            store.setInMemoryTrail(next);
            expect(store.trail()).toEqual([
                { inode: A.inode, title: A.title },
                { inode: B.inode, title: B.title }
            ]);
        }));

        it('appends to an existing trail', fakeAsync(() => {
            router.navigateByUrl('/content/inode-b?rc=inode-a,inode-b');
            tick();

            expect(store.appendToTrail(B, C)).toEqual([A.inode, B.inode, C.inode]);
        }));
    });

    describe('setInMemoryTrail (dialog / overlay)', () => {
        it('overrides the URL trail when set', fakeAsync(() => {
            router.navigateByUrl('/content/inode-x?rc=inode-a,inode-b');
            tick();

            store.setInMemoryTrail(['m1', 'm2']);
            expect(store.trailInodes()).toEqual(['m1', 'm2']);
        }));

        it('treats an empty in-memory trail as authoritative (not the URL)', fakeAsync(() => {
            router.navigateByUrl('/content/inode-x?rc=inode-a,inode-b');
            tick();

            store.setInMemoryTrail([]);
            expect(store.trailInodes()).toEqual([]);
        }));

        it('reverts to the URL trail when cleared with null', fakeAsync(() => {
            router.navigateByUrl('/content/inode-x?rc=inode-a,inode-b');
            tick();

            store.setInMemoryTrail(['m1']);
            store.setInMemoryTrail(null);
            expect(store.trailInodes()).toEqual([A.inode, B.inode]);
        }));
    });

    describe('buildTrailForSavedInode', () => {
        it('repoints the current (last) crumb to the new inode after a save', fakeAsync(() => {
            router.navigateByUrl('/content/inode-c?rc=inode-a,inode-b,inode-c');
            tick();

            expect(store.buildTrailForSavedInode('inode-c2')).toBe('inode-a,inode-b,inode-c2');
        }));

        it('returns null when there is no active trail', fakeAsync(() => {
            router.navigateByUrl('/content/inode-a');
            tick();

            expect(store.buildTrailForSavedInode('inode-a2')).toBeNull();
        }));
    });
});
