import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConvertToBlockInfoComponent } from './dot-convert-to-block-info.component';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.wysiwyg.info.content': 'Info Content',
    'contenttypes.field.properties.wysiwyg.info.button': 'Info Button',
    'learn-more': 'Learn More'
});

describe('DotConvertToBlockInfoComponent', () => {
    let spectator: Spectator<DotConvertToBlockInfoComponent>;

    const createComponent = createComponentFactory({
        component: DotConvertToBlockInfoComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render info and learn more button', () => {
        const infoContent = spectator.query(byTestId('infoContent'));
        const learnMore = spectator.query(byTestId('learnMore'));

        expect(infoContent?.textContent?.trim()).toBe('Info Content');
        expect(learnMore?.textContent?.trim()).toBe('Learn More');
    });

    it('should render info and info button when currentField has id', () => {
        spectator.setInput('currentField', { id: '123' });
        spectator.detectChanges();

        const infoContent = spectator.query(byTestId('infoContent'));
        const button = spectator.query(byTestId('button'));

        expect(infoContent).toBeTruthy();
        expect(infoContent?.textContent?.trim()).toBe('Info Content');
        expect(button).toBeTruthy();
        expect(button?.textContent?.trim()).toBe('Info Button');
    });
});
