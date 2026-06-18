import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorHeaderComponent } from './dot-image-editor-header.component';

describe('DotImageEditorHeaderComponent', () => {
    let spectator: Spectator<DotImageEditorHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotImageEditorHeaderComponent,
        imports: [ButtonModule, DotMessagePipe]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the title', () => {
        const header = spectator.query(byTestId('image-editor-header'));

        expect(header).toBeTruthy();
        expect(header).toHaveText('edit.content.image-editor.title');
    });

    it('should expose the header and close testids', () => {
        expect(spectator.query(byTestId('image-editor-header'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-close-btn'))).toBeTruthy();
    });

    it('should emit close when the close button is clicked', () => {
        const closeSpy = jest.spyOn(spectator.component.close, 'emit');
        const button = spectator.query(byTestId('image-editor-close-btn'))?.querySelector('button');

        spectator.click(button as HTMLElement);

        expect(closeSpy).toHaveBeenCalledTimes(1);
    });
});
