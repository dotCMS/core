import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormsModule, NgControl } from '@angular/forms';

import { DotTrimInputDirective } from '@dotcms/ui';

const STRING_WITH_SPACES = '   Test Value   ';

@Component({
    template: `
        <input [(ngModel)]="name" dotTrimInput data-testId="input-to-trim" />
    `
})
export class DotTrimInputHostMockComponent {
    name = STRING_WITH_SPACES;
}

describe('DotTrimInputDirective', () => {
    let spectator: Spectator<DotTrimInputHostMockComponent>;
    const createComponent = createComponentFactory({
        component: DotTrimInputHostMockComponent,
        imports: [FormsModule, DotTrimInputDirective],
        providers: [NgControl]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should trim the input value on blur', async () => {
        const input = spectator.query(byTestId('input-to-trim')) as HTMLInputElement;
        const expectedValue = STRING_WITH_SPACES.trim();

        await spectator.fixture.whenStable();

        expect(spectator.query(byTestId('input-to-trim'))).toExist();
        expect(input.value).toBe(STRING_WITH_SPACES);

        spectator.dispatchFakeEvent(input, 'blur');
        spectator.detectComponentChanges();

        expect(input.value).toBe(expectedValue);
    });
});
