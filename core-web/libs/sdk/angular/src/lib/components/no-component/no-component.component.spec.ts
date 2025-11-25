import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { NoComponent } from './no-component.component';

import { DotCMSContentlet } from '../../models';

describe('NoComponentComponent', () => {
    let spectator: Spectator<NoComponent>;

    const createComponent = createComponentFactory(NoComponent);

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: { contentType: 'exampleContentType' } as DotCMSContentlet
            }
        });
    });

    it('should display the content type', () => {
        const noComponent = spectator.debugElement.nativeElement;
        expect(noComponent?.innerHTML).toContain('No Component for exampleContentType');
    });
});
