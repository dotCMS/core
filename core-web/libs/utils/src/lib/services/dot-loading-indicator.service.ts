import { signal, Service } from '@angular/core';

@Service()
export class DotLoadingIndicatorService {
    display = signal(false);

    show(): void {
        this.display.set(true);
    }

    hide(): void {
        this.display.set(false);
    }
}
