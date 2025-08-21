import { createPipeFactory, SpectatorPipe } from '@ngneat/spectator/jest';

import { Component, Input } from '@angular/core';

import { DotNameFormatPipe } from './name-format.pipe';

@Component({
    standalone: false,
    template: `
        <div>{{ value | dotNameFormat }}</div>
    `
})
class CustomHostComponent {
    @Input() public value: string | null = null;
}

describe('DotNameFormatPipe', () => {
    let spectator: SpectatorPipe<DotNameFormatPipe>;
    const createPipe = createPipeFactory({
        pipe: DotNameFormatPipe,
        host: CustomHostComponent
    });

    it('should return empty string when input is null', () => {
        spectator = createPipe({
            hostProps: { value: null }
        });
        expect(spectator.element).toHaveText('');
    });

    it('should return empty string when input is undefined', () => {
        spectator = createPipe({
            hostProps: { value: undefined }
        });
        expect(spectator.element).toHaveText('');
    });

    it('should return original value when input has only one word', () => {
        spectator = createPipe({
            hostProps: { value: 'John' }
        });
        expect(spectator.element).toHaveText('John');
    });

    it('should format name with initial and last name', () => {
        spectator = createPipe({
            hostProps: { value: 'John Doe' }
        });
        expect(spectator.element).toHaveText('J. Doe');
    });
    it('should format name with initial and last name', () => {
        spectator = createPipe({
            hostProps: { value: 'Admin User' }
        });
        expect(spectator.element).toHaveText('A. User');
    });

    it('should format name with multiple last names when they are short', () => {
        spectator = createPipe({
            hostProps: { value: 'John Doe Smith' }
        });
        expect(spectator.element).toHaveText('J. Smith');
    });

    it('should format name with initials when last name is longer than limit', () => {
        spectator = createPipe({
            hostProps: { value: 'John Rodríguez-González' }
        });
        expect(spectator.element).toHaveText('J. R.');
    });

    it('should handle extra spaces correctly', () => {
        spectator = createPipe({
            hostProps: { value: '  John   Doe  ' }
        });
        expect(spectator.element).toHaveText('J. Doe');
    });

    it('should handle empty string', () => {
        spectator = createPipe({
            hostProps: { value: '' }
        });
        expect(spectator.element).toHaveText('');
    });

    it('should format all names as initials when total length exceeds limit', () => {
        spectator = createPipe({
            hostProps: { value: 'John William Rodriguez Smith' }
        });
        expect(spectator.element).toHaveText('J. R.');
    });

    it('should respect custom max length for longer names', () => {
        spectator = createPipe(`{{ value | dotNameFormat:15 }}`, {
            hostProps: { value: 'John William Rodriguez' }
        });
        expect(spectator.element).toHaveText('J. Rodriguez');
    });

    it('should respect custom max length for shorter names', () => {
        spectator = createPipe(`{{ value | dotNameFormat:5 }}`, {
            hostProps: { value: 'John Doe' }
        });
        expect(spectator.element).toHaveText('J. D.');
    });

    it('should use default max length when no length is provided', () => {
        spectator = createPipe(`{{ value | dotNameFormat }}`, {
            hostProps: { value: 'John Rodriguez' }
        });
        expect(spectator.element).toHaveText('J. R.');
    });
});
