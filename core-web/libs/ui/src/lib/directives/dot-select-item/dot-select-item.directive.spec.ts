import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';

import { DotSelectItemDirective } from './dot-select-item.directive';

describe('DotSelectItemDirective', () => {
    let spectator: SpectatorDirective<DotSelectItemDirective>;
    let autoComplete: AutoComplete;
    let onKeyUpMock: jasmine.Spy;
    let onOptionSelect: jasmine.Spy;

    const createDirective = createDirectiveFactory({
        directive: DotSelectItemDirective,
        template: `<p-autoComplete dotSelectItem [suggestions]="[]" />`,
        imports: [AutoCompleteModule]
    });

    beforeEach(() => {
        spectator = createDirective();
        autoComplete = spectator.query(AutoComplete);
        onKeyUpMock = spyOn(spectator.directive, 'onKeyUp').and.callThrough();
        onOptionSelect = spyOn(autoComplete, 'onOptionSelect').and.callThrough();
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

        expect(onOptionSelect).toHaveBeenCalledOnceWith(event, event.target.value);
    });

    it('should not call autoComplete selectItem when key is not "Enter"', () => {
        const event = {
            key: 'K',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(onOptionSelect).not.toHaveBeenCalled();
    });
});
