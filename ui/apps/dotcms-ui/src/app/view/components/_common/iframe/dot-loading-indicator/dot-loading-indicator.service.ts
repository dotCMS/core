import { Injectable } from '@angular/core';

@Injectable()
export class DotLoadingIndicatorService {
    display = false;

    show(): void {
        this.display = true;
    }

    hide(): void {
        this.display = false;
    }
}
