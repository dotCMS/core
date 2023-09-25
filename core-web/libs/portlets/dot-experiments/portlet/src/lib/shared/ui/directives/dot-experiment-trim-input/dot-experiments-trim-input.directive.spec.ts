import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { FormsModule, NgControl } from '@angular/forms';

import { DotExperimentsTrimInputDirective } from './dot-experiments-trim-input.directive';

const STRING_WITH_SPACES = '   Test Value   ';

@Component({
    template: `<input [(ngModel)]="name" dotTrimInput data-testId="input-to-trim" />`
})
export class DotTrimInputHostMockComponent {
    name = STRING_WITH_SPACES;
}

describe('DotExperimentsTrimInputDirective', () => {
    let spectator: Spectator<DotTrimInputHostMockComponent>;
    const createComponent = createComponentFactory({
        component: DotTrimInputHostMockComponent,
        imports: [FormsModule, DotExperimentsTrimInputDirective],
        providers: [NgControl]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should trim the input value on blur', () => {
        const input = spectator.query(byTestId('input-to-trim')) as HTMLInputElement;
        const expectedValue = STRING_WITH_SPACES.trim();

        expect(spectator.query(byTestId('input-to-trim'))).toExist();
        expect(input.value).toBe(STRING_WITH_SPACES);

        spectator.dispatchFakeEvent(input, 'blur');

        expect(input.value).toBe(expectedValue);
    });
});
