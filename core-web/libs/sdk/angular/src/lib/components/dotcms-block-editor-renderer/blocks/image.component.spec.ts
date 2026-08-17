import { createComponentFactory, Spectator } from '@openng/spectator';

import { DotImageBlock } from './image.component';

describe('DotImageBlock', () => {
    let spectator: Spectator<DotImageBlock>;

    const createComponent = createComponentFactory({
        component: DotImageBlock,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render figure with img', () => {
        spectator.setInput('attrs', { src: 'image.png', alt: 'test' });
        spectator.detectChanges();
        expect(spectator.query('figure')).toBeTruthy();
        expect(spectator.query('img')).toBeTruthy();
    });

    it('should set img src from attrs', () => {
        spectator.setInput('attrs', { src: 'image.png', alt: 'alt text' });
        spectator.detectChanges();
        expect(spectator.query<HTMLImageElement>('img')?.src).toContain('image.png');
    });

    it('should apply float-left style when textWrap is left', () => {
        spectator.setInput('attrs', { src: 'image.png', alt: '', textWrap: 'left' });
        spectator.detectChanges();
        const figure = spectator.query<HTMLElement>('figure');
        expect(figure?.style.float).toBe('left');
    });

    it('should apply float-right style when textWrap is right', () => {
        spectator.setInput('attrs', { src: 'image.png', alt: '', textWrap: 'right' });
        spectator.detectChanges();
        const figure = spectator.query<HTMLElement>('figure');
        expect(figure?.style.float).toBe('right');
    });

    it('should apply text-align style when textAlign is set', () => {
        spectator.setInput('attrs', { src: 'image.png', alt: '', textAlign: 'center' });
        spectator.detectChanges();
        const figure = spectator.query<HTMLElement>('figure');
        expect(figure?.style.textAlign).toBe('center');
    });

    it('should have no style when neither textWrap nor textAlign is set', () => {
        spectator.setInput('attrs', { src: 'image.png', alt: '' });
        spectator.detectChanges();
        const figure = spectator.query<HTMLElement>('figure');
        expect(figure?.getAttribute('style')).toBeFalsy();
    });

    describe('image link', () => {
        it('should wrap the img in an anchor when href is set', () => {
            spectator.setInput('attrs', { src: 'image.png', alt: 'alt', href: '/about-us' });
            spectator.detectChanges();
            const anchor = spectator.query<HTMLAnchorElement>('a');
            expect(anchor).toBeTruthy();
            expect(anchor?.getAttribute('href')).toBe('/about-us');
            expect(anchor?.querySelector('img')).toBeTruthy();
        });

        it('should keep the anchor inside the figure so wrapper styles still apply', () => {
            spectator.setInput('attrs', {
                src: 'image.png',
                alt: 'alt',
                href: '/about-us',
                textWrap: 'left'
            });
            spectator.detectChanges();
            const figure = spectator.query<HTMLElement>('figure');
            expect(figure?.style.float).toBe('left');
            expect(figure?.querySelector('a > img')).toBeTruthy();
        });

        it('should set target when the link opens in a new tab', () => {
            spectator.setInput('attrs', {
                src: 'image.png',
                alt: 'alt',
                href: 'https://dotcms.com',
                target: '_blank'
            });
            spectator.detectChanges();
            expect(spectator.query('a')?.getAttribute('target')).toBe('_blank');
        });

        it('should add rel noopener noreferrer when target is _blank', () => {
            spectator.setInput('attrs', {
                src: 'image.png',
                alt: 'alt',
                href: 'https://dotcms.com',
                target: '_blank'
            });
            spectator.detectChanges();
            expect(spectator.query('a')?.getAttribute('rel')).toBe('noopener noreferrer');
        });

        it('should not add rel or target when the link opens in the same tab', () => {
            spectator.setInput('attrs', { src: 'image.png', alt: 'alt', href: '/about-us' });
            spectator.detectChanges();
            const anchor = spectator.query<HTMLAnchorElement>('a');
            expect(anchor?.getAttribute('rel')).toBeNull();
            expect(anchor?.getAttribute('target')).toBeNull();
        });

        it('should render a bare img when href is null', () => {
            spectator.setInput('attrs', { src: 'image.png', alt: 'alt', href: null });
            spectator.detectChanges();
            expect(spectator.query('a')).toBeNull();
            expect(spectator.query('figure > img')).toBeTruthy();
        });

        it('should render a bare img when href is an empty string (link unset in the editor)', () => {
            spectator.setInput('attrs', { src: 'image.png', alt: 'alt', href: '' });
            spectator.detectChanges();
            expect(spectator.query('a')).toBeNull();
            expect(spectator.query('figure > img')).toBeTruthy();
        });
    });
});
