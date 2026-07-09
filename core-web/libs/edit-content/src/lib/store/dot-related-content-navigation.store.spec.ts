import { provideLocationMocks } from '@angular/common/testing';
import { Component } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import {
    DotRelatedContentNavigationStore,
    toRelatedContentCrumbs
} from './dot-related-content-navigation.store';

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

        it('yields an empty trail when rc is repeated (router hands back a string[])', fakeAsync(() => {
            // A repeated query param makes Angular return an array; the typeof guard
            // must not blow up on .split.
            router.navigateByUrl('/content/inode-c?rc=inode-a&rc=inode-b');
            tick();

            expect(store.trailInodes()).toEqual([]);
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
        it('seeds [current, target] when the current trail is empty and registers titles', () => {
            const next = store.appendToTrail([], A, B);
            expect(next).toEqual([A.inode, B.inode]);

            // Titles were registered, so toRelatedContentCrumbs labels them.
            expect(toRelatedContentCrumbs(next, store.titleCache())).toEqual([
                { inode: A.inode, title: A.title },
                { inode: B.inode, title: B.title }
            ]);
        });

        it('appends to the passed-in current trail (caller owns it)', () => {
            expect(store.appendToTrail([A.inode, B.inode], B, C)).toEqual([
                A.inode,
                B.inode,
                C.inode
            ]);
        });
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
