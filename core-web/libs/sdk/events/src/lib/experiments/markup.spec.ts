import { describe, expect, it } from 'vitest';

import { HIDING_RULE } from './constants';
import { experimentMarkup } from './markup';

describe('experimentMarkup', () => {
    it('returns nothing for a page that runs no experiment', () => {
        expect(experimentMarkup({ runningExperimentId: null })).toBeNull();
        expect(experimentMarkup(undefined)).toBeNull();
    });

    it('marks the experiment and the variant the server rendered, with the rule and the script', () => {
        const markup = experimentMarkup({
            runningExperimentId: 'experiment-1',
            viewAs: { variantId: 'variant-1' }
        });

        expect(markup?.attributes).toEqual({
            'data-dot-experiment': 'experiment-1',
            'data-dot-variant': 'variant-1'
        });
        expect(markup?.style).toBe(HIDING_RULE);
        expect(markup?.script).toContain('"id":"experiment-1","rendered":"variant-1"');
    });

    it('marks the default variant when the page names none', () => {
        const markup = experimentMarkup({ runningExperimentId: 'experiment-1' });

        expect(markup?.attributes['data-dot-variant']).toBe('DEFAULT');
    });
});
