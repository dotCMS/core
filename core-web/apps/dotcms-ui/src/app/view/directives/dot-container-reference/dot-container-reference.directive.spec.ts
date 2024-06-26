/* eslint-disable @typescript-eslint/no-empty-function */
import {
    ComponentRef,
    ElementRef,
    EmbeddedViewRef,
    Injector,
    ViewContainerRef,
    ViewRef
} from '@angular/core';

import { DotContainerReferenceDirective } from './dot-container-reference.directive';

class TestViewContainerRef extends ViewContainerRef {
    readonly element: ElementRef;
    readonly injector: Injector;
    readonly length: number;
    readonly parentInjector: Injector;

    clear(): void {}

    createComponent<C>(): ComponentRef<C> {
        return undefined;
    }

    createEmbeddedView<C>(): EmbeddedViewRef<C> {
        return undefined;
    }

    detach(): ViewRef | null {
        return undefined;
    }

    get(): ViewRef | null {
        return undefined;
    }

    indexOf(): number {
        return 0;
    }

    insert(): ViewRef {
        return undefined;
    }

    move(): ViewRef {
        return undefined;
    }

    remove(): void {}
}

describe('DotContainerReferenceDirective', () => {
    it('should create an instance', () => {
        const directive = new DotContainerReferenceDirective(new TestViewContainerRef());
        expect(directive).toBeTruthy();
    });
});
