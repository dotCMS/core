import { EXPERIMENT_RETURN_PARAM, EXPERIMENT_RETURN_PORTLET } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { buildPageEditorLink, buildVariantEditorLink } from './dot-experiments-uve-link.util';

import { DotExperimentConfigurePage } from '../shared/models';

/**
 * The outbound leg of the variant round-trip (#37005, FR-002, FR-003, FR-004).
 *
 * Contract under test: `specs/37005-experiments-uve-integration/contracts/navigation-destinations.md` §2.
 *
 * The builder returns a `{ commands, queryParams }` pair rather than navigating, so the exact URL
 * and every parameter can be asserted without a router. It returns `null` — never a partial pair —
 * when the page data cannot produce a complete destination, which is what lets the caller refuse
 * with a reason instead of navigating somewhere plausible and wrong.
 */
const PAGE: DotExperimentConfigurePage = {
    pageId: 'page-1',
    title: 'Pricing',
    path: '/pricing/index',
    languageId: 2
};

const VARIANT_ID = 'variant-b';
const EXPERIMENT_ID = 'exp-1';

describe('buildVariantEditorLink', () => {
    it('should target /edit-page/content', () => {
        const link = buildVariantEditorLink({
            page: PAGE,
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.EDIT
        });

        expect(link?.commands).toEqual(['/edit-page/content']);
    });

    it('should carry every parameter UVE needs, sourced from the page data', () => {
        const link = buildVariantEditorLink({
            page: PAGE,
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.EDIT
        });

        expect(link?.queryParams).toEqual({
            url: '/pricing/index',
            language_id: 2,
            'com.dotmarketing.persona.id': expect.any(String),
            variantName: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.EDIT,
            [EXPERIMENT_RETURN_PARAM]: EXPERIMENT_RETURN_PORTLET
        });
    });

    // The page's real language, not the guard's default of 1. A page in language 2 opened at
    // language_id=1 renders different content and nothing reports an error.
    it('should send the page language, never a hardcoded 1', () => {
        const link = buildVariantEditorLink({
            page: { ...PAGE, languageId: 7 },
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.EDIT
        });

        expect(link?.queryParams.language_id).toBe(7);
    });

    // FR-005's return leg reads this marker to decide where the chip lands. Without it, a
    // round-trip begun in the portlet would fall back to the switch and, with the switch off,
    // return the editor to the legacy screen they never came from.
    it('should mark the origin as the portlet so the return leg can resolve it', () => {
        const link = buildVariantEditorLink({
            page: PAGE,
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.PREVIEW
        });

        expect(link?.queryParams[EXPERIMENT_RETURN_PARAM]).toBe(EXPERIMENT_RETURN_PORTLET);
    });

    it.each([UVE_MODE.EDIT, UVE_MODE.PREVIEW])('should pass mode %s through verbatim', (mode) => {
        const link = buildVariantEditorLink({
            page: PAGE,
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode
        });

        expect(link?.queryParams.mode).toBe(mode);
    });

    // UVE declares `?mode=` as UVE_MODE (`DotPageApiParams.mode`) and compares it as
    // `mode === UVE_MODE.EDIT`, so UVE_MODE is the canonical type here. The legacy card sends
    // DotPageMode and works only because DotPageMode.EDIT/PREVIEW happen to carry the same two
    // strings; the enums diverge at LIVE ('ADMIN_MODE' vs 'LIVE'). Pinning the literals keeps that
    // coincidence from quietly becoming the contract for whoever adds a third mode.
    it('should send the literal wire values UVE compares against', () => {
        const edit = buildVariantEditorLink({
            page: PAGE,
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.EDIT
        });
        const preview = buildVariantEditorLink({
            page: PAGE,
            variantId: VARIANT_ID,
            experimentId: EXPERIMENT_ID,
            mode: UVE_MODE.PREVIEW
        });

        expect(edit?.queryParams.mode).toBe('EDIT_MODE');
        expect(preview?.queryParams.mode).toBe('PREVIEW_MODE');
    });

    describe('refusal (FR-004) — returns null rather than a partial destination', () => {
        it('should refuse when there is no page', () => {
            expect(
                buildVariantEditorLink({
                    page: null,
                    variantId: VARIANT_ID,
                    experimentId: EXPERIMENT_ID,
                    mode: UVE_MODE.EDIT
                })
            ).toBeNull();
        });

        it.each(['', '   ', undefined])('should refuse when path is %p', (path) => {
            expect(
                buildVariantEditorLink({
                    page: { ...PAGE, path: path as string },
                    variantId: VARIANT_ID,
                    experimentId: EXPERIMENT_ID,
                    mode: UVE_MODE.EDIT
                })
            ).toBeNull();
        });

        // The one refusal that would otherwise be a silent wrong-language open, because
        // editEmaGuard fills a missing language_id with 1 instead of rejecting.
        it.each([undefined, null])('should refuse when languageId is %p', (languageId) => {
            expect(
                buildVariantEditorLink({
                    page: { ...PAGE, languageId: languageId as number },
                    variantId: VARIANT_ID,
                    experimentId: EXPERIMENT_ID,
                    mode: UVE_MODE.EDIT
                })
            ).toBeNull();
        });

        it('should refuse when there is no variant id', () => {
            expect(
                buildVariantEditorLink({
                    page: PAGE,
                    variantId: '',
                    experimentId: EXPERIMENT_ID,
                    mode: UVE_MODE.EDIT
                })
            ).toBeNull();
        });

        it('should refuse when there is no experiment id — the return leg needs it', () => {
            expect(
                buildVariantEditorLink({
                    page: PAGE,
                    variantId: VARIANT_ID,
                    experimentId: '',
                    mode: UVE_MODE.EDIT
                })
            ).toBeNull();
        });
    });

    // FR-003. The legacy card re-parsed window.location.href for a `url=` fragment; this builder
    // must be indifferent to the address bar. Asserted by giving the ambient URL a misleading
    // value and checking the output is unmoved.
    it('should ignore window.location entirely', () => {
        const original = window.location.href;
        window.history.replaceState({}, '', '/experiments?filter=someone-elses-page&orderby=name');

        try {
            const link = buildVariantEditorLink({
                page: PAGE,
                variantId: VARIANT_ID,
                experimentId: EXPERIMENT_ID,
                mode: UVE_MODE.EDIT
            });

            expect(link?.queryParams.url).toBe('/pricing/index');
            expect(JSON.stringify(link)).not.toContain('someone-elses-page');
        } finally {
            window.history.replaceState({}, '', original);
        }
    });
});

/**
 * The same builder without the variant, experiment and mode params — the list's page-filter chip
 * uses it to send the editor back to the page they came from (FR-024).
 */
describe('buildPageEditorLink', () => {
    it('should target the page in the editor with no experiment context', () => {
        const link = buildPageEditorLink(PAGE);

        expect(link?.commands).toEqual(['/edit-page/content']);
        expect(link?.queryParams).toEqual({
            url: '/pricing/index',
            language_id: 2,
            'com.dotmarketing.persona.id': expect.any(String)
        });
    });

    it('should carry no variant, experiment, mode or origin marker', () => {
        const link = buildPageEditorLink(PAGE);

        expect(link?.queryParams).not.toHaveProperty('variantName');
        expect(link?.queryParams).not.toHaveProperty('experimentId');
        expect(link?.queryParams).not.toHaveProperty('mode');
        expect(link?.queryParams).not.toHaveProperty(EXPERIMENT_RETURN_PARAM);
    });

    it('should refuse on the same incomplete page data', () => {
        expect(buildPageEditorLink(null)).toBeNull();
        expect(buildPageEditorLink({ ...PAGE, path: '' })).toBeNull();
        expect(
            buildPageEditorLink({ ...PAGE, languageId: undefined as unknown as number })
        ).toBeNull();
    });
});
