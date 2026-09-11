import { MenuItem } from 'primeng/api';

import {
    experimentConfigureCrumb,
    experimentResultsCrumb,
    experimentsListCrumb,
    putCrumbOnTrail
} from './dot-experiments-breadcrumb.util';

const PAGE_ASSET_ID = 'a9f30020-54ef-494e-92ed-645e757171c2';
const EXPERIMENT_ID = '211e12ca-23cc-4647-a3de-c030ca9cb971';

/** The narrowing as the screens hand it over: the two keys the list reads as its page filter. */
const PAGE_FILTER = { pageId: PAGE_ASSET_ID, language_id: 2 };

describe('dot-experiments-breadcrumb.util', () => {
    describe('experimentsListCrumb', () => {
        // The crumb trail renders `url` into an `href`, and dotAdmin runs on hash location — so
        // the address has to carry the `/dotAdmin/#` prefix the router's own serialisation omits.
        it('should address the list through the admin hash prefix', () => {
            expect(experimentsListCrumb('Experiments List', {}).url).toBe(
                '/dotAdmin/#/experiments'
            );
        });

        // Arriving from UVE the list is narrowed to one page; the crumb must return to THAT list,
        // not to the site-wide one, or stepping back through the trail widens the filter silently.
        it('should keep the page filter when there is one', () => {
            expect(experimentsListCrumb('Experiments List', PAGE_FILTER).url).toBe(
                `/dotAdmin/#/experiments?pageId=${PAGE_ASSET_ID}&language_id=2`
            );
        });

        // A stable id is what stops a reload appending a second copy: `addNewBreadcrumb` replaces
        // the last crumb when the id matches, and only appends otherwise.
        it('should carry a stable id and open in the same window', () => {
            const crumb = experimentsListCrumb('Experiments List', PAGE_FILTER);

            expect(crumb.id).toBe('experiments-list');
            expect(crumb.label).toBe('Experiments List');
            expect(crumb.target).toBe('_self');
        });

        it('should keep the same id whether or not the list is filtered', () => {
            expect(experimentsListCrumb('Experiments List', {}).id).toBe(
                experimentsListCrumb('Experiments List', PAGE_FILTER).id
            );
        });
    });

    describe('experimentConfigureCrumb', () => {
        it('should address the Configure screen through the admin hash prefix', () => {
            expect(experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, {}).url).toBe(
                `/dotAdmin/#/experiments/${EXPERIMENT_ID}/configuration`
            );
        });

        // The draft has no address of its own yet, and the crumb still has to lead back to the
        // screen the editor is on.
        it('should address the new-experiment route while the draft has no id', () => {
            expect(experimentConfigureCrumb('New Experiment', null, {}).url).toBe(
                '/dotAdmin/#/experiments/new'
            );
        });

        // Same reason as the back button: a crumb that dropped the filter would leave the screen
        // it returns to pointing at the site-wide list.
        it('should keep the page narrowing in the address', () => {
            expect(experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, PAGE_FILTER).url).toBe(
                `/dotAdmin/#/experiments/${EXPERIMENT_ID}/configuration?pageId=${PAGE_ASSET_ID}&language_id=2`
            );
        });

        // Deliberately NOT the experiment's id: creating the draft swaps `/experiments/new` for
        // the experiment's own address without leaving the screen, and a rename rewrites the label
        // on every keystroke. With the experiment as the id, the first of those would append a
        // second crumb ("New Experiment › Summer Test") instead of replacing the first.
        it('should keep one id across the creation swap and the rename', () => {
            const draft = experimentConfigureCrumb('New Experiment', null, {});
            const created = experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, {});

            expect(draft.id).toBe(created.id);
            expect(created.label).toBe('Summer Test');
            expect(created.target).toBe('_self');
        });
    });

    describe('experimentResultsCrumb', () => {
        it('should address the Results screen through the admin hash prefix', () => {
            expect(experimentResultsCrumb('Summer Test', EXPERIMENT_ID, PAGE_FILTER).url).toBe(
                `/dotAdmin/#/experiments/${EXPERIMENT_ID}/results?pageId=${PAGE_ASSET_ID}&language_id=2`
            );
        });

        // A different id from Configure's, so stepping Configure → Results appends rather than
        // replacing: both screens are on the trail, in the order they were visited.
        it('should identify itself apart from the Configure crumb', () => {
            const results = experimentResultsCrumb('Summer Test', EXPERIMENT_ID, {});

            expect(results.id).toBe('experiments-results');
            expect(results.id).not.toBe(
                experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, {}).id
            );
            expect(results.label).toBe('Summer Test');
            expect(results.target).toBe('_self');
        });
    });

    /**
     * The reason this function exists at all.
     *
     * `addNewBreadcrumb` **skips** an item whose id or url matches the last crumb's — it does not
     * replace it, whatever its name suggests. Every crumb in this file is written more than once
     * (a rename rewrites the label on each keystroke; creating a draft swaps `/experiments/new`
     * for the experiment's own address; a site switch widens the list's), so going through
     * `addNewBreadcrumb` alone left the trail showing the first version forever: the label the
     * screen opened with, and a link back to an address that no longer exists.
     */
    describe('putCrumbOnTrail', () => {
        /** The trail as a list, since append-vs-replace is the whole question here. */
        const trailOf = (...items: MenuItem[]) => {
            const trail = [...items];

            return {
                trail,
                lastBreadcrumb: () => trail.at(-1) ?? null,
                addNewBreadcrumb: (crumb: MenuItem) => {
                    trail.push(crumb);
                },
                setLastBreadcrumb: (crumb: MenuItem) => {
                    trail[trail.length - 1] = crumb;
                }
            };
        };

        it('should append when the last crumb belongs to somebody else', () => {
            const store = trailOf({ id: 'page-crumb', label: 'Home' });

            putCrumbOnTrail(store, experimentsListCrumb('Experiments List', {}));

            expect(store.trail.map(({ label }) => label)).toEqual(['Home', 'Experiments List']);
        });

        it('should append onto an empty trail', () => {
            const store = trailOf();

            putCrumbOnTrail(store, experimentsListCrumb('Experiments List', {}));

            expect(store.trail).toHaveLength(1);
        });

        // The rename case: same crumb, new label, and no second copy of it.
        it('should rewrite in place when the last crumb is the same one', () => {
            const store = trailOf(
                { id: 'page-crumb', label: 'Home' },
                experimentConfigureCrumb('New Experiment', null, {})
            );

            putCrumbOnTrail(store, experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, {}));

            expect(store.trail.map(({ label }) => label)).toEqual(['Home', 'Summer Test']);
        });

        // The creation swap: the label may be unchanged while the address is not, and the old
        // address pointed at a draft that no longer exists.
        it('should carry the new address through a rewrite', () => {
            const store = trailOf(experimentConfigureCrumb('Summer Test', null, {}));

            putCrumbOnTrail(store, experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, {}));

            expect(store.trail).toHaveLength(1);
            expect(store.trail[0].url).toBe(
                `/dotAdmin/#/experiments/${EXPERIMENT_ID}/configuration`
            );
        });

        // The list's own path: clearing the narrowing changes that crumb's address, and the skip
        // had been discarding it.
        it('should rewrite the list crumb when its narrowing changes', () => {
            const store = trailOf(experimentsListCrumb('Experiments List', { pageId: 'page-1' }));

            putCrumbOnTrail(store, experimentsListCrumb('Experiments List', {}));

            expect(store.trail).toHaveLength(1);
            expect(store.trail[0].url).toBe('/dotAdmin/#/experiments');
        });

        // Results sits on top of Configure rather than replacing it: different ids, so the trail
        // keeps both screens in the order they were visited.
        it('should append Results on top of Configure rather than replacing it', () => {
            const store = trailOf(experimentConfigureCrumb('Summer Test', EXPERIMENT_ID, {}));

            putCrumbOnTrail(store, experimentResultsCrumb('Summer Test', EXPERIMENT_ID, {}));

            expect(store.trail.map(({ id }) => id)).toEqual([
                'experiments-configure',
                'experiments-results'
            ]);
        });
    });
});
