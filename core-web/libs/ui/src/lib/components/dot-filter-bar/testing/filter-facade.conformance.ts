import { DotFilterFacade, DotFilterValue } from '../filter-facade.token';

/**
 * What a surface must supply for its {@link DotFilterFacade} implementation to be checked.
 *
 * The probes exist because some obligations are only observable *below* the facade: whether a key
 * was deleted rather than blanked, whether paging actually reset, whether a value survived the
 * surface's own encoding. The facade deliberately cannot answer those about itself.
 */
export interface DotFilterFacadeConformanceSetup {
    /** The implementation under test, freshly constructed. */
    facade: DotFilterFacade;

    /**
     * The raw filter set beneath the facade, in the surface's own encoding.
     *
     * Needed for O3: "the key is gone" and "the key is present holding `undefined`" are
     * indistinguishable through `getFilterValue`, and only one of them is correct.
     */
    readRawBag: () => Record<string, unknown>;

    /** Current 1-based page, for O4 and O9. */
    readPage: () => number;

    /** Moves off page 1, so a reset is observable. */
    goToPage2: () => void;

    /**
     * What `clearFilters()` must land on, in normalized terms — this surface's defaults, which are
     * never simply `{}`. Content Drive: its environment language plus shared-assets-on. The
     * picker: whatever the caller seeded, plus the same shared-assets default.
     */
    expectedDefaults: Record<string, DotFilterValue>;

    /**
     * One filter key whose value crosses an encoding boundary on this surface, with a normalized
     * value to round-trip through it. Content Drive supplies `baseType` with base-type *names*
     * (stored as numbers); the picker supplies the same key with names (stored as names), which is
     * the point — the same test passes on both for different reasons.
     */
    encodedFilter: { key: string; value: DotFilterValue };

    /**
     * Writes a value into the raw bag, bypassing the facade, so O7 can prove an unmappable stored
     * value is dropped on the way out rather than passed through raw.
     */
    writeRaw: (key: string, value: unknown) => void;

    /**
     * A stored value for `encodedFilter.key` that this surface cannot map back.
     *
     * Only meaningful where {@link DotFilterFacadeConformanceCapabilities.normalizes} is true — a
     * surface whose normalization is the identity has no such value.
     */
    unmappableRawValue?: unknown;

    /**
     * Keys that name a caller restriction rather than a filter — a media-type narrowing, a pinned
     * version state. The facade must not expose them (O8).
     *
     * Empty on a surface that has no restrictions, which is Content Drive.
     */
    restrictedKeys: string[];
}

/**
 * What the surface can be checked for, known at `describe` registration time.
 *
 * Separate from the setup because Jest builds its test tree before any `beforeEach` runs: a test
 * guarded on a probe read out of the setup object would silently never register, since the setup
 * has not been built yet.
 */
export interface DotFilterFacadeConformanceCapabilities {
    /**
     * Whether this surface's normalization is a real translation rather than the identity.
     *
     * Content Drive encodes base types as the numeric keys its URL needs, so it has values it
     * cannot map back and O7's "drop it rather than pass it through" is a genuine obligation. The
     * AssetPicker stores the normalized vocabulary directly — its normalization *is* identity — so
     * "unmappable" is not a state it can be in, and asserting otherwise would be inventing a
     * requirement to satisfy a test.
     */
    normalizes: boolean;
}

/**
 * The executable form of the `DOT_FILTER_FACADE` contract — obligations **O1–O9** in
 * `specs/37174-shared-picker-toolbar/contracts/filter-facade.contract.md` §2.
 *
 * Every surface that implements the facade runs this against its own implementation. That is the
 * whole point: the two surfaces store filters differently (Content Drive encodes base types as the
 * numeric keys its URL needs; the AssetPicker keeps names because it has no URL), and without one
 * shared suite the two encodings drift silently — which is the failure this feature exists to end.
 *
 * Scope note: these are the obligations the *facade* can guarantee. Two things deliberately live
 * elsewhere. Assertions about the outgoing search request — that no filter combination widens past
 * a caller restriction, that a removed key leaves the request byte-identical — belong to each
 * surface's store spec, because the facade does not build the request. So does Content Drive's URL
 * round-trip: it is a property of that store's serialization, already covered by its own specs, and
 * reproducing the router machinery here would test the harness rather than the contract.
 *
 * @param name Surface name, used in the `describe` title so a failure names the offender.
 * @param setup Builds a fresh facade plus probes. Called in a `beforeEach`, so each test starts clean.
 * @param capabilities What this surface can be checked for. Read at registration time, before any
 *   `beforeEach` — see {@link DotFilterFacadeConformanceCapabilities}.
 */
export function testFilterFacadeConformance(
    name: string,
    setup: () => DotFilterFacadeConformanceSetup,
    capabilities: DotFilterFacadeConformanceCapabilities = { normalizes: false }
): void {
    describe(`DotFilterFacade conformance: ${name}`, () => {
        let ctx: DotFilterFacadeConformanceSetup;
        let facade: DotFilterFacade;

        beforeEach(() => {
            ctx = setup();
            facade = ctx.facade;
        });

        describe('O1 — round-trip', () => {
            it('should return an equal value for one written through the facade', () => {
                facade.patchFilters({ [ctx.encodedFilter.key]: ctx.encodedFilter.value });

                expect(facade.getFilterValue(ctx.encodedFilter.key)).toEqual(
                    ctx.encodedFilter.value
                );
            });

            it('should round-trip a plain string filter', () => {
                facade.patchFilters({ title: 'quarterly report' });

                expect(facade.getFilterValue('title')).toBe('quarterly report');
            });

            it('should round-trip a multi-value filter', () => {
                facade.patchFilters({ languageId: ['1', '2'] });

                expect(facade.getFilterValue('languageId')).toEqual(['1', '2']);
            });
        });

        describe('O2 — absence is undefined', () => {
            it('should return undefined for a key that was never set', () => {
                expect(facade.getFilterValue('a-key-nobody-set')).toBeUndefined();
            });

            it('should distinguish "not filtered" from "filtered to nothing selected"', () => {
                facade.patchFilters({ contentType: [] });

                // Both are falsy in a naive check, and conflating them fires an empty state before
                // the editor has chosen anything.
                expect(facade.getFilterValue('contentType')).toEqual([]);
                expect(facade.getFilterValue('contentType')).not.toBeUndefined();
            });

            it('should report the ENCODED key as undefined once it is not set', () => {
                // The key that crosses this surface's encoding boundary is the one most likely to
                // lose the distinction, because a decoder that maps a list naturally returns an
                // empty list for nothing. Probing only a plain key left exactly that hole.
                //
                // Removed rather than simply read: a surface may legitimately *seed* this key from
                // its caller's config, so "never set" is not a state every implementation starts in.
                facade.removeFilter(ctx.encodedFilter.key);

                expect(facade.getFilterValue(ctx.encodedFilter.key)).toBeUndefined();
            });

            it('should report the encoded key as an empty array once set to nothing', () => {
                facade.patchFilters({ [ctx.encodedFilter.key]: ctx.encodedFilter.value });
                facade.patchFilters({ [ctx.encodedFilter.key]: [] });

                expect(facade.getFilterValue(ctx.encodedFilter.key)).toEqual([]);
            });
        });

        describe('O3 — removal deletes', () => {
            it('should drop the key from the underlying set rather than blanking it', () => {
                facade.patchFilters({ title: 'draft' });

                facade.removeFilter('title');

                expect(Object.keys(ctx.readRawBag())).not.toContain('title');
            });

            it('should report the removed filter as absent', () => {
                facade.patchFilters({ title: 'draft' });

                facade.removeFilter('title');

                expect(facade.getFilterValue('title')).toBeUndefined();
            });

            it('should tolerate removing a filter that was never set', () => {
                expect(() => facade.removeFilter('never-set')).not.toThrow();
            });
        });

        describe('O4 — every write resets paging', () => {
            it('should return to the first page after patchFilters', () => {
                ctx.goToPage2();

                facade.patchFilters({ title: 'anything' });

                expect(ctx.readPage()).toBe(1);
            });

            it('should return to the first page after removeFilter', () => {
                facade.patchFilters({ title: 'anything' });
                ctx.goToPage2();

                facade.removeFilter('title');

                expect(ctx.readPage()).toBe(1);
            });

            it('should return to the first page after clearFilters', () => {
                ctx.goToPage2();

                facade.clearFilters();

                expect(ctx.readPage()).toBe(1);
            });
        });

        describe('O5 — clearFilters restores defaults, not emptiness', () => {
            it("should land on this surface's defaults", () => {
                facade.patchFilters({ title: 'something', contentType: ['Blog'] });

                facade.clearFilters();

                for (const [key, value] of Object.entries(ctx.expectedDefaults)) {
                    expect(facade.getFilterValue(key)).toEqual(value);
                }
            });

            it('should leave nothing worth clearing afterwards', () => {
                facade.patchFilters({ title: 'something' });

                facade.clearFilters();

                expect(facade.$hasNonDefaultFilters()).toBe(false);
            });

            it('should not carry over a filter the editor had set', () => {
                facade.patchFilters({ contentType: ['Blog'] });

                facade.clearFilters();

                expect(facade.getFilterValue('contentType')).toBeUndefined();
            });
        });

        describe('O6 — $hasNonDefaultFilters ignores defaults', () => {
            it('should be false on a fresh surface, despite the defaults being present', () => {
                expect(facade.$hasNonDefaultFilters()).toBe(false);
            });

            it('should be true once a filter differs from its default', () => {
                facade.patchFilters({ contentType: ['Blog'] });

                expect(facade.$hasNonDefaultFilters()).toBe(true);
            });

            it('should be false when a filter is explicitly set to its own default value', () => {
                // Selecting the default by hand is indistinguishable from the seeded state, and
                // clearing it would just re-select the same thing.
                facade.patchFilters(ctx.expectedDefaults);

                expect(facade.$hasNonDefaultFilters()).toBe(false);
            });

            it('should be true for a search term alone', () => {
                facade.patchFilters({ title: 'report' });

                expect(facade.$hasNonDefaultFilters()).toBe(true);
            });
        });

        describe('O7 — normalization is total and lossless', () => {
            it('should hand back the normalized vocabulary, not the stored encoding', () => {
                facade.patchFilters({ [ctx.encodedFilter.key]: ctx.encodedFilter.value });

                expect(facade.getFilterValue(ctx.encodedFilter.key)).toEqual(
                    ctx.encodedFilter.value
                );
            });

            if (capabilities.normalizes) {
                it('should drop a stored value it cannot map rather than passing it through raw', () => {
                    ctx.writeRaw(ctx.encodedFilter.key, ctx.unmappableRawValue);

                    const normalized = facade.getFilterValue(ctx.encodedFilter.key);

                    expect(normalized).not.toEqual(ctx.unmappableRawValue);
                    if (Array.isArray(normalized)) {
                        expect(normalized).toEqual([]);
                    }
                });
            }
        });

        describe('O8 — restrictions are unreachable', () => {
            it('should not expose any caller restriction as a filter value', () => {
                for (const key of ctx.restrictedKeys) {
                    expect(facade.getFilterValue(key)).toBeUndefined();
                }
            });

            it('should not count a caller restriction as something to clear', () => {
                // A restriction is part of what the surface *is*. If it registered here, the
                // picker would offer "Clear all" the moment it opened.
                expect(facade.$hasNonDefaultFilters()).toBe(false);
            });
        });

        describe('O9 — writes are idempotent', () => {
            it('should not reset paging for a patch that changes nothing', () => {
                facade.patchFilters({ contentType: ['Blog'] });
                ctx.goToPage2();

                facade.patchFilters({ contentType: ['Blog'] });

                expect(ctx.readPage()).toBe(2);
            });

            it('should not reset paging when removing a filter that was never set', () => {
                ctx.goToPage2();

                facade.removeFilter('never-set');

                expect(ctx.readPage()).toBe(2);
            });
        });
    });
}
