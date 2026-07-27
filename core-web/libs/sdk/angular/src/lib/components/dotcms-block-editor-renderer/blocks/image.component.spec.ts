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
});
