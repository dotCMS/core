import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { DotSelectSeoToolComponent } from './dot-select-seo-tool.component';

import { SEO_MEDIA_TYPES } from '../../../content/services/dot-edit-content-html/models/meta-tags-model';

describe('DotSelectSeoToolComponent', () => {
    let spectator: Spectator<DotSelectSeoToolComponent>;
    const createComponent = createComponentFactory({
        component: DotSelectSeoToolComponent,
        imports: [CommonModule],
        declareComponent: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                socialMedia: SEO_MEDIA_TYPES.FACEBOOK
            }
        });
    });

    it('should display social media preview tile when socialMedia input is provided', () => {
        spectator.setInput({
            socialMedia: SEO_MEDIA_TYPES.FACEBOOK
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('select-seo-tool-item'))).toExist();
        expect(spectator.query(byTestId('select-seo-tool-icon'))).toHaveClass('pi-facebook');
        expect(spectator.query(byTestId('select-seo-tool-title'))).toHaveText('Facebook');
        expect(spectator.query(byTestId('select-seo-tool-details'))).toHaveText(
            'Social Media Preview Tile'
        );
    });

    it('should display device preview tile when device input is provided', () => {
        spectator.setInput({
            socialMedia: null
        });
        spectator.setInput({
            device: {
                inode: '0',
                identifier: '',
                name: 'iPad Pro',
                cssHeight: '1024',
                cssWidth: '1366',
                icon: 'pi pi-tablet'
            }
        });

        spectator.detectChanges();
        expect(spectator.query(byTestId('select-seo-tool-item'))).toExist();
        expect(spectator.query(byTestId('select-seo-tool-icon'))).toHaveClass('pi-tablet');
        expect(spectator.query(byTestId('select-seo-tool-title'))).toHaveText('iPad Pro');
        expect(spectator.query(byTestId('select-seo-tool-details'))).toHaveText('1,024 x 1,366');
    });
});
