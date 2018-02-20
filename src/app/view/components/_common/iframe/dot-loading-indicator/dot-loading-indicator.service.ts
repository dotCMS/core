import { Injectable } from '@angular/core';

@Injectable()
export class DotLoadingIndicatorService {
    display = false;

    constructor() {}

    show(): void {
        this.display = true;
    }

    hide(): void {
        this.display = false;
    }
}
