import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DotLoadingIndicatorService {
    display = signal(false);

    show(): void {
        this.display.set(true);
    }

    hide(): void {
        this.display.set(false);
    }
}
