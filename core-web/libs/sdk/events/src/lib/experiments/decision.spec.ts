import { describe, expect, it } from 'vitest';

import { decideVariant } from './decision';

import type { VariantDecisionInput } from './decision';

const EXPERIMENT = 'experiment-1';
const VARIANT = 'dotexperiment-experiment-variant-1';
const NOW = 1_000_000;

const input = (overrides: Partial<VariantDecisionInput> = {}): VariantDecisionInput => ({
    experimentId: EXPERIMENT,
    rendered: 'DEFAULT',
    href: 'https://site.test/page',
    stored: null,
    disabled: false,
    now: NOW,
    param: 'variantName',
    defaultVariant: 'DEFAULT',
    ...overrides
});

const assignedTo = (variant: string, expiresAt = NOW + 1) => ({
    experiments: [{ id: EXPERIMENT, variant: { name: variant }, expiresAt }],
    evaluatedIds: [EXPERIMENT]
});

describe('decideVariant', () => {
    it('is unknown when nothing is stored about the experiment', () => {
        expect(decideVariant(input())).toEqual({ kind: 'unknown' });
        expect(decideVariant(input({ stored: { experiments: 'broken' } }))).toEqual({
            kind: 'unknown'
        });
    });

    it('excludes the visitor while experiments are off', () => {
        expect(decideVariant(input({ stored: assignedTo(VARIANT), disabled: true }))).toEqual({
            kind: 'excluded'
        });
    });

    it('excludes a visitor evaluated before and not assigned', () => {
        expect(
            decideVariant(input({ stored: { experiments: [], evaluatedIds: [EXPERIMENT] } }))
        ).toEqual({
            kind: 'excluded'
        });
    });

    it('ignores an expired assignment', () => {
        expect(
            decideVariant(
                input({
                    stored: {
                        experiments: [
                            { id: EXPERIMENT, variant: { name: VARIANT }, expiresAt: NOW }
                        ]
                    }
                })
            )
        ).toEqual({
            kind: 'unknown'
        });
    });

    it("is assigned when the server rendered the visitor's variant", () => {
        expect(decideVariant(input({ stored: assignedTo('DEFAULT') }))).toEqual({
            kind: 'assigned',
            variant: 'DEFAULT'
        });
    });

    it('redirects to the assigned variant, with the parameter set', () => {
        expect(
            decideVariant(
                input({ stored: assignedTo(VARIANT), href: 'https://site.test/page?utm=a' })
            )
        ).toEqual({
            kind: 'redirect',
            variant: VARIANT,
            url: `https://site.test/page?utm=a&variantName=${VARIANT}`
        });
    });

    it('redirects to the default variant without the parameter', () => {
        expect(
            decideVariant(
                input({
                    stored: assignedTo('DEFAULT'),
                    rendered: VARIANT,
                    href: `https://site.test/page?variantName=${VARIANT}`
                })
            )
        ).toEqual({ kind: 'redirect', variant: 'DEFAULT', url: 'https://site.test/page' });
    });

    it('is ignored when the URL already asks for the assigned variant but the server rendered another', () => {
        expect(
            decideVariant(
                input({
                    stored: assignedTo(VARIANT),
                    href: `https://site.test/page?variantName=${VARIANT}`
                })
            )
        ).toEqual({ kind: 'ignored', variant: VARIANT });
        expect(decideVariant(input({ stored: assignedTo('DEFAULT'), rendered: VARIANT }))).toEqual({
            kind: 'ignored',
            variant: 'DEFAULT'
        });
    });

    it('references nothing outside itself, so the boot script can print it', () => {
        // Run from its own source, detached from this module
        const detached = new Function(
            `return (${decideVariant.toString()})`
        )() as typeof decideVariant;

        expect(detached(input({ stored: assignedTo(VARIANT) }))).toEqual(
            decideVariant(input({ stored: assignedTo(VARIANT) }))
        );
    });
});
