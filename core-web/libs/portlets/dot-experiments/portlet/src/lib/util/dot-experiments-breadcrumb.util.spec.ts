import { experimentConfigureCrumb, experimentsListCrumb } from './dot-experiments-breadcrumb.util';

const PAGE_ASSET_ID = 'a9f30020-54ef-494e-92ed-645e757171c2';
const EXPERIMENT_ID = '211e12ca-23cc-4647-a3de-c030ca9cb971';

describe('dot-experiments-breadcrumb.util', () => {
    describe('experimentsListCrumb', () => {
        // The crumb trail renders `url` into an `href`, and dotAdmin runs on hash location — so
        // the address has to carry the `/dotAdmin/#` prefix the router's own serialisation omits.
        it('should address the list through the admin hash prefix', () => {
            expect(experimentsListCrumb('Experiments List', null).url).toBe(
                '/dotAdmin/#/experiments'
            );
        });

        // Arriving from UVE the list is narrowed to one page; the crumb must return to THAT list,
        // not to the site-wide one, or stepping back through the trail widens the filter silently.
        it('should keep the page filter when there is one', () => {
            expect(experimentsListCrumb('Experiments List', PAGE_ASSET_ID).url).toBe(
                `/dotAdmin/#/experiments?pageAsset=${PAGE_ASSET_ID}`
            );
        });

        // A stable id is what stops a reload appending a second copy: `addNewBreadcrumb` replaces
        // the last crumb when the id matches, and only appends otherwise.
        it('should carry a stable id and open in the same window', () => {
            const crumb = experimentsListCrumb('Experiments List', PAGE_ASSET_ID);

            expect(crumb.id).toBe('experiments-list');
            expect(crumb.label).toBe('Experiments List');
            expect(crumb.target).toBe('_self');
        });

        it('should keep the same id whether or not the list is filtered', () => {
            expect(experimentsListCrumb('Experiments List', null).id).toBe(
                experimentsListCrumb('Experiments List', PAGE_ASSET_ID).id
            );
        });
    });

    describe('experimentConfigureCrumb', () => {
        it('should address the Configure screen through the admin hash prefix', () => {
            expect(experimentConfigureCrumb('asd', EXPERIMENT_ID).url).toBe(
                `/dotAdmin/#/experiments/${EXPERIMENT_ID}/configuration`
            );
        });

        // The experiment's own id, so renaming it replaces the crumb instead of stacking a second
        // one beside the old name.
        it('should identify the crumb by the experiment', () => {
            const crumb = experimentConfigureCrumb('asd', EXPERIMENT_ID);

            expect(crumb.id).toBe(EXPERIMENT_ID);
            expect(crumb.label).toBe('asd');
            expect(crumb.target).toBe('_self');
        });
    });
});
