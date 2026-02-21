import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotLinkComponent } from './dot-link.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const messageServiceMock = new MockDotMessageService({
    'dot.common.testing': 'This is a test'
});

const defaultProps = {
    href: 'api/v1/123',
    icon: 'pi-link',
    label: 'dot.common.testing'
};

describe('DotLinkComponent', () => {
    let spectator: Spectator<DotLinkComponent>;

    const createComponent = createComponentFactory({
        component: DotLinkComponent,
        imports: [DotMessagePipe, DotLinkComponent],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    function getLinkElement(): HTMLAnchorElement | null {
        return spectator.query('a');
    }

    beforeEach(() => {
        spectator = createComponent({
            props: defaultProps
        });
        spectator.detectChanges();
    });

    it('should show label', () => {
        const link = getLinkElement();
        expect(link?.textContent?.trim()).toBe('This is a test');
    });

    it('should set link properties and attr correctly', () => {
        const link = getLinkElement();
        expect(link?.getAttribute('target')).toBe('_blank');
        expect(link?.getAttribute('href')).toBe('/api/v1/123');
        expect(link?.getAttribute('title')).toBe('/api/v1/123');
    });

    it('should update link when href is change', () => {
        spectator = createComponent({
            props: { ...defaultProps, href: '/api/new/1000' }
        });
        spectator.detectChanges();

        const link = getLinkElement();
        expect(link?.getAttribute('href')).toBe('/api/new/1000');
        expect(link?.getAttribute('title')).toBe('/api/new/1000');
    });

    it('should set the link relative always', () => {
        spectator = createComponent({
            props: { ...defaultProps, href: 'api/no/start/slash' }
        });
        spectator.detectChanges();

        const link = getLinkElement();
        expect(link?.getAttribute('href')).toBe('/api/no/start/slash');
        expect(link?.getAttribute('title')).toBe('/api/no/start/slash');
    });
});
