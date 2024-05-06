import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator';

import { By } from '@angular/platform-browser';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';

import { DotSelectItemDirective } from './dot-select-item.directive';

describe('DotSelectItemDirective', () => {
    let spectator: SpectatorDirective<DotSelectItemDirective>;
    let autoComplete: AutoComplete;
    let onKeyUpMock: jasmine.Spy;
    let selectItem: jasmine.Spy;

    const createDirective = createDirectiveFactory({
        directive: DotSelectItemDirective,
        template: `<p-autoComplete dotSelectItem></p-autoComplete>`,
        imports: [AutoCompleteModule]
    });

    beforeEach(() => {
        spectator = createDirective();
        autoComplete = spectator.debugElement.query(By.css('p-autoComplete')).componentInstance;
        onKeyUpMock = spyOn(spectator.directive, 'onKeyUp').and.callThrough();
        selectItem = spyOn(autoComplete, 'selectItem');
    });

    it('should call onKeyUp from the directive', () => {
        const event = {
            key: 'Enter',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(onKeyUpMock).toHaveBeenCalledOnceWith(event);
    });

    it('should call autoComplete selectItem when key is "Enter"', () => {
        const event = {
            key: 'Enter',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(selectItem).toHaveBeenCalledOnceWith(event.target.value);
    });

    it('should not call autoComplete selectItem when key is not "Enter"', () => {
        const event = {
            key: 'K',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(selectItem).not.toHaveBeenCalled();
    });
});
