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
});
