import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';

import { DotSelectItemDirective } from './dot-select-item.directive';

describe('DotSelectItemDirective', () => {
    let spectator: SpectatorDirective<DotSelectItemDirective>;
    let autoComplete: AutoComplete;
    let onKeyUpMock: jest.SpyInstance;
    let onOptionSelect: jest.SpyInstance;

    const createDirective = createDirectiveFactory({
        directive: DotSelectItemDirective,
        template: `<p-autoComplete dotSelectItem [suggestions]="[]" />`,
        imports: [AutoCompleteModule]
    });

    beforeEach(() => {
        spectator = createDirective();
        autoComplete = spectator.query(AutoComplete);
        onKeyUpMock = jest.spyOn(spectator.directive, 'onKeyUp');
        onOptionSelect = jest.spyOn(autoComplete, 'onOptionSelect');
    });

    it('should call onKeyUp from the directive', () => {
        const event = {
            key: 'Enter',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(onKeyUpMock).toHaveBeenCalledWith(event);
        expect(onKeyUpMock).toHaveBeenCalledTimes(1);
    });

    it('should call autoComplete selectItem when key is "Enter"', () => {
        const event = {
            key: 'Enter',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(onOptionSelect).toHaveBeenCalledWith(event, event.target.value);
        expect(onOptionSelect).toHaveBeenCalledTimes(1);
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
