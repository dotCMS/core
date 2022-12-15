import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DotLoadingIndicatorService {
    display = false;

    show(): void {
        this.display = true;
    }

    hide(): void {
        this.display = false;
    }
}
