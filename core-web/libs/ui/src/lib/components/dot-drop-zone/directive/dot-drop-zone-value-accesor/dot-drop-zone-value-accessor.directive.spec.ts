import { createDirectiveFactory, SpectatorDirective } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotDropZoneValueAccessorDirective } from './dot-drop-zone-value-accessor.directive';

import { DotDropZoneComponent } from '../../dot-drop-zone.component';

describe('DotDropZoneValueAccessorDirective', () => {
    let spectator: SpectatorDirective<DotDropZoneValueAccessorDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotDropZoneValueAccessorDirective,
        declarations: [MockComponent(DotDropZoneComponent)],
        providers: [
            {
                multi: true,
                provide: NG_VALUE_ACCESSOR,
                useExisting: forwardRef(() => DotDropZoneValueAccessorDirective)
            }
        ]
    });

    describe('Used with dot-drop-zone component', () => {
        beforeEach(() => {
            spectator = createDirective(`
            <dot-drop-zone dotDropZoneValueAccessor>
                <div id="dot-drop-zone__content" class="dot-drop-zone__content">
                    Content
                </div>
            </dot-drop-zone>`);
        });

        it('should create', () => {
            expect(spectator.directive).toBeTruthy();
        });
    });

    describe('Used without dot-drop-zone component', () => {
        it('should throw error if not inside dot-drop-zone', () => {
            expect(() => {
                spectator = createDirective(`
                <div dotDropZoneValueAccessor>
                    <div id="dot-drop-zone__content" class="dot-drop-zone__content">
                        Content
                    </div>
                </div>`);
            }).toThrowError(
                'dot-drop-zone-value-accessor can only be used inside of a dot-drop-zone'
            );
        });
    });
});
