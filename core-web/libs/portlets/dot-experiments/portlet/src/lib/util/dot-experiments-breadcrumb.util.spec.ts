import {
    experimentConfigureCrumb,
    experimentResultsCrumb,
    experimentsListCrumb
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
});
