import { Component, inject } from '@angular/core';

import { DotErrorHandler } from './dot-error-handler.service';

@Component({
    selector: 'dot-error-boundary',
    styles: [
        `
            :host {
                display: contents;
            }

            /* Error message styling */
            .error-message {
                padding: 10px;
                border: 1px solid red;
                background-color: rgba(255, 0, 0, 0.1);
                color: red;
            }
        `
    ],
    template: `
        @if ($error(); as error) {
            <div class="error-message">
                {{ error.message }}
            </div>
        } @else {
            <ng-content></ng-content>
        }
    `,
    providers: [DotErrorHandler],
    standalone: true
})
export class DotErrorBoundaryComponent {
    errorHandler = inject(DotErrorHandler);

    $error = this.errorHandler.errorSignal;
}
