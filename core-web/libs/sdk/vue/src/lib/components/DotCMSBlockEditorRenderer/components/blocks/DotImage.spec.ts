import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import type { BlockEditorNode } from '@dotcms/types';

import DotImage from './DotImage.vue';

const imageNode = (attrs: Record<string, unknown>): BlockEditorNode =>
    ({ type: 'dotImage', attrs }) as unknown as BlockEditorNode;

const mountImage = (attrs: Record<string, unknown>) =>
    mount(DotImage, { props: { node: imageNode(attrs) } });

describe('DotImage', () => {
    it('renders a figure with the img src and alt', () => {
        const wrapper = mountImage({ src: 'image.png', alt: 'a picture' });
        expect(wrapper.find('figure').exists()).toBe(true);
        const img = wrapper.find('img');
        expect(img.attributes('src')).toBe('image.png');
        expect(img.attributes('alt')).toBe('a picture');
    });

    it('applies the float style when textWrap is set', () => {
        const wrapper = mountImage({ src: 'image.png', alt: '', textWrap: 'right' });
        expect(wrapper.find('figure').attributes('style')).toContain('float: right');
    });

    it('applies the text-align style when textAlign is set', () => {
        const wrapper = mountImage({ src: 'image.png', alt: '', textAlign: 'center' });
        expect(wrapper.find('figure').attributes('style')).toContain('text-align: center');
    });

    describe('image link', () => {
        it('wraps the img in an anchor when href is set', () => {
            const wrapper = mountImage({ src: 'image.png', alt: 'alt', href: '/about-us' });
            const anchor = wrapper.find('figure > a');
            expect(anchor.exists()).toBe(true);
            expect(anchor.attributes('href')).toBe('/about-us');
            expect(anchor.find('img').attributes('src')).toBe('image.png');
        });

        it('keeps the wrapper style on the figure when the image is linked', () => {
            const wrapper = mountImage({
                src: 'image.png',
                alt: 'alt',
                href: '/about-us',
                textWrap: 'right'
            });
            expect(wrapper.find('figure').attributes('style')).toContain('float: right');
            expect(wrapper.find('figure > a > img').exists()).toBe(true);
        });

        it('sets target and rel when the link opens in a new tab', () => {
            const wrapper = mountImage({
                src: 'image.png',
                alt: 'alt',
                href: 'https://dotcms.com',
                target: '_blank'
            });
            const anchor = wrapper.find('a');
            expect(anchor.attributes('target')).toBe('_blank');
            expect(anchor.attributes('rel')).toBe('noopener noreferrer');
        });

        it('omits target and rel when the link opens in the same tab', () => {
            const wrapper = mountImage({ src: 'image.png', alt: 'alt', href: '/about-us' });
            const anchor = wrapper.find('a');
            expect(anchor.attributes('target')).toBeUndefined();
            expect(anchor.attributes('rel')).toBeUndefined();
        });

        it('renders a bare img when href is null', () => {
            const wrapper = mountImage({ src: 'image.png', alt: 'alt', href: null });
            expect(wrapper.find('a').exists()).toBe(false);
            expect(wrapper.find('figure > img').exists()).toBe(true);
        });

        it('renders a bare img when href is an empty string (link unset in the editor)', () => {
            const wrapper = mountImage({ src: 'image.png', alt: 'alt', href: '' });
            expect(wrapper.find('a').exists()).toBe(false);
            expect(wrapper.find('figure > img').exists()).toBe(true);
        });
    });
});
