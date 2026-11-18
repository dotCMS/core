import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { DotCMSExperiment } from './DotCMSExperiment';

const EXPERIMENT_PAGE = { runningExperimentId: 'experiment-1', viewAs: { variantId: 'variant-1' } };

describe('DotCMSExperiment', () => {
    it('renders only its children when the page runs no experiment', () => {
        const { container } = render(
            <DotCMSExperiment page={{ runningExperimentId: null }}>
                <p>Blog list</p>
            </DotCMSExperiment>
        );

        expect(container.innerHTML).toBe('<p>Blog list</p>');
    });

    it('marks its content with the experiment and the variant the server rendered', () => {
        render(
            <DotCMSExperiment page={EXPERIMENT_PAGE} className="blog">
                <p>Blog list</p>
            </DotCMSExperiment>
        );

        const wrapper = screen.getByText('Blog list').parentElement;
        expect(wrapper?.getAttribute('data-dot-experiment')).toBe('experiment-1');
        expect(wrapper?.getAttribute('data-dot-variant')).toBe('variant-1');
        expect(wrapper?.className).toBe('blog');
    });

    it('prints the hiding rule and the boot script before its content', () => {
        const { container } = render(
            <DotCMSExperiment page={EXPERIMENT_PAGE}>
                <p>Blog list</p>
            </DotCMSExperiment>
        );

        // Before the content, so the content is hidden as soon as the browser parses it
        const [style, script] = Array.from(container.children);
        expect(style?.tagName).toBe('STYLE');
        expect(style?.textContent).toContain('[data-dot-experiment]{visibility:hidden');
        expect(script?.tagName).toBe('SCRIPT');
        expect(script?.innerHTML).toContain('"id":"experiment-1","rendered":"variant-1"');
    });

    it('gives the script the nonce', () => {
        const { container } = render(
            <DotCMSExperiment page={EXPERIMENT_PAGE} nonce="abc123">
                <p>Blog list</p>
            </DotCMSExperiment>
        );

        expect(container.querySelector('script')?.getAttribute('nonce')).toBe('abc123');
    });

    it('gives the hiding rule the nonce too, so a nonce-based CSP applies it', () => {
        const { container } = render(
            <DotCMSExperiment page={EXPERIMENT_PAGE} nonce="abc123">
                <p>Blog list</p>
            </DotCMSExperiment>
        );

        expect(container.querySelector('style')?.getAttribute('nonce')).toBe('abc123');
    });
});
