import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { NoComponentComponent } from './no-component.component';

import { DotCMSContentlet } from '../../models';

describe('NoComponentComponent', () => {
    let spectator: Spectator<NoComponentComponent>;

    const createComponent = createComponentFactory(NoComponentComponent);

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: { contentType: 'exampleContentType' } as DotCMSContentlet
            }
        });
    });

    it('should display the content type', () => {
        const noComponent = spectator.debugElement.nativeElement;
        expect(noComponent?.innerHTML).toBe('No Component for exampleContentType');
    });
});
