import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { BlockEditorNode } from '@dotcms/types';

import { DotCMSImage } from '../../components/DotCMSBlockEditorRenderer/components/blocks/Image';

const baseNode = (attrs: Record<string, unknown>): BlockEditorNode => ({
    type: 'dotImage',
    attrs
});

describe('DotCMSImage', () => {
    it('should render figure and img', () => {
        const { container } = render(
            <DotCMSImage node={baseNode({ src: 'image.png', alt: 'alt' })} />
        );
        expect(container.querySelector('figure')).toBeInTheDocument();
        expect(container.querySelector('img')).toBeInTheDocument();
    });

    it('should set img src and alt', () => {
        render(<DotCMSImage node={baseNode({ src: 'image.png', alt: 'my alt' })} />);
        const img = screen.getByAltText('my alt');
        expect(img).toHaveAttribute('src', 'image.png');
    });

    it('should apply float-left style when textWrap is left', () => {
        const { container } = render(
            <DotCMSImage node={baseNode({ src: 'img.png', alt: '', textWrap: 'left' })} />
        );
        const figure = container.querySelector('figure') as HTMLElement;
        expect(figure.style.float).toBe('left');
        expect(figure.style.width).toBe('50%');
    });

    it('should apply float-right style when textWrap is right', () => {
        const { container } = render(
            <DotCMSImage node={baseNode({ src: 'img.png', alt: '', textWrap: 'right' })} />
        );
        const figure = container.querySelector('figure') as HTMLElement;
        expect(figure.style.float).toBe('right');
    });

    it('should apply textAlign style when textAlign is set', () => {
        const { container } = render(
            <DotCMSImage node={baseNode({ src: 'img.png', alt: '', textAlign: 'center' })} />
        );
        const figure = container.querySelector('figure') as HTMLElement;
        expect(figure.style.textAlign).toBe('center');
    });

    it('should apply maxWidth style on img when textWrap is set', () => {
        const { container } = render(
            <DotCMSImage node={baseNode({ src: 'img.png', alt: '', textWrap: 'left' })} />
        );
        const img = container.querySelector('img') as HTMLElement;
        expect(img.style.maxWidth).toBe('100%');
    });

    it('should have no wrapper style when neither textWrap nor textAlign is set', () => {
        const { container } = render(<DotCMSImage node={baseNode({ src: 'img.png', alt: '' })} />);
        const figure = container.querySelector('figure') as HTMLElement;
        expect(figure.getAttribute('style')).toBeFalsy();
    });

    describe('image link', () => {
        it('should wrap the img in an anchor when href is set', () => {
            const { container } = render(
                <DotCMSImage node={baseNode({ src: 'img.png', alt: 'alt', href: '/about-us' })} />
            );
            const anchor = container.querySelector('a') as HTMLAnchorElement;
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', '/about-us');
            expect(anchor.querySelector('img')).toBeInTheDocument();
        });

        it('should keep the anchor inside the figure so wrapper styles still apply', () => {
            const { container } = render(
                <DotCMSImage
                    node={baseNode({
                        src: 'img.png',
                        alt: 'alt',
                        href: '/about-us',
                        textWrap: 'left'
                    })}
                />
            );
            const figure = container.querySelector('figure') as HTMLElement;
            expect(figure.style.float).toBe('left');
            expect(figure.querySelector('a > img')).toBeInTheDocument();
        });

        it('should set target when the link opens in a new tab', () => {
            const { container } = render(
                <DotCMSImage
                    node={baseNode({
                        src: 'img.png',
                        alt: 'alt',
                        href: 'https://dotcms.com',
                        target: '_blank'
                    })}
                />
            );
            expect(container.querySelector('a')).toHaveAttribute('target', '_blank');
        });

        it('should add rel noopener noreferrer when target is _blank', () => {
            const { container } = render(
                <DotCMSImage
                    node={baseNode({
                        src: 'img.png',
                        alt: 'alt',
                        href: 'https://dotcms.com',
                        target: '_blank'
                    })}
                />
            );
            expect(container.querySelector('a')).toHaveAttribute('rel', 'noopener noreferrer');
        });

        it('should not add rel when the link opens in the same tab', () => {
            const { container } = render(
                <DotCMSImage node={baseNode({ src: 'img.png', alt: 'alt', href: '/about-us' })} />
            );
            const anchor = container.querySelector('a') as HTMLAnchorElement;
            expect(anchor).not.toHaveAttribute('rel');
            expect(anchor).not.toHaveAttribute('target');
        });

        it('should render a bare img when href is null', () => {
            const { container } = render(
                <DotCMSImage node={baseNode({ src: 'img.png', alt: 'alt', href: null })} />
            );
            expect(container.querySelector('a')).not.toBeInTheDocument();
            expect(container.querySelector('figure > img')).toBeInTheDocument();
        });

        it('should render a bare img when href is an empty string (link unset in the editor)', () => {
            const { container } = render(
                <DotCMSImage node={baseNode({ src: 'img.png', alt: 'alt', href: '' })} />
            );
            expect(container.querySelector('a')).not.toBeInTheDocument();
            expect(container.querySelector('figure > img')).toBeInTheDocument();
        });
    });
});
