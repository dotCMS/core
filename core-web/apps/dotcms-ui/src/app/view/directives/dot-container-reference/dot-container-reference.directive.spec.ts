import { mockProvider, createDirectiveFactory } from '@ngneat/spectator';

import { ViewContainerRef } from '@angular/core';

import { DotContainerReferenceDirective } from './dot-container-reference.directive';

describe('DotContainerReferenceDirective', () => {
    const createDirective = createDirectiveFactory({
        directive: DotContainerReferenceDirective,
        providers: [mockProvider(ViewContainerRef)],
        detectChanges: false
    });

    it('should create an instance', () => {
        const spectator = createDirective(`<div dotContainerReference></div>`);
        spectator.detectChanges();
        expect(spectator).toBeTruthy();
        expect(spectator.directive.viewContainerRef).toBeTruthy();
    });
});
