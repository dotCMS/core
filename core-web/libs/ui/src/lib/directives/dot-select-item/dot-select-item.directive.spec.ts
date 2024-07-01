import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';

import { DotSelectItemDirective } from './dot-select-item.directive';

describe('DotSelectItemDirective', () => {
    let spectator: SpectatorDirective<DotSelectItemDirective>;
    let autoComplete: AutoComplete;
    let onKeyUpMock: jest.SpyInstance;
    let selectItem: jest.SpyInstance;

    const createDirective = createDirectiveFactory({
        directive: DotSelectItemDirective,
        template: `<p-autoComplete dotSelectItem></p-autoComplete>`,
        imports: [AutoCompleteModule]
    });

    beforeEach(() => {
        spectator = createDirective();
        autoComplete = spectator.query(AutoComplete);
        onKeyUpMock = jest.spyOn(spectator.directive, 'onKeyUp').mockImplementation();
        selectItem = jest.spyOn(autoComplete, 'selectItem');
    });

    it('should call onKeyUp from the directive', () => {
        const event = {
            key: 'Enter',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(onKeyUpMock).toHaveBeenCalledWith(event);
    });

    it('should call autoComplete selectItem when key is "Enter"', () => {
        const event = {
            key: 'Enter',
            target: { value: 'test' }
        };

        spectator.triggerEventHandler('p-autoComplete[dotSelectItem]', 'onKeyUp', event);

        expect(selectItem).toHaveBeenCalledWith(event.target.value);
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
