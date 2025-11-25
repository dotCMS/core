import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { SEO_MEDIA_TYPES } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSelectSeoToolComponent } from './dot-select-seo-tool.component';

describe('DotSelectSeoToolComponent', () => {
    let spectator: Spectator<DotSelectSeoToolComponent>;
    const createComponent = createComponentFactory({
        component: DotSelectSeoToolComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'seo.rules.media.preview.tile': 'Social Media Preview Tile',
                    'seo.rules.media.search.engine': 'Search Engine Results Page'
                })
            }
        ],
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

    it('should display social media preview tile when socialMedia input is provided', () => {
        spectator.setInput({
            socialMedia: SEO_MEDIA_TYPES.GOOGLE
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('select-seo-tool-item'))).toExist();
        expect(spectator.query(byTestId('select-seo-tool-icon'))).toHaveClass('pi-google');
        expect(spectator.query(byTestId('select-seo-tool-title'))).toHaveText('Google');
        expect(spectator.query(byTestId('select-seo-tool-details'))).toHaveText(
            'Search Engine Results Page'
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
