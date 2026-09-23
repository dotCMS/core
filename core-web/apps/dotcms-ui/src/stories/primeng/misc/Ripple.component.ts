import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';

import { PrimeNG } from 'primeng/config';

@Component({
    changeDetection: ChangeDetectionStrategy.Eager,
    selector: 'dot-p-button-ripple',
    template: `
        <button class="p-button-success" type="button" pButton pRipple>
            <span pButtonLabel>Success</span>
        </button>
    `
})
export class RippleComponent implements OnInit {
    private primengConfig = inject(PrimeNG);

    ngOnInit() {
        this.primengConfig.ripple.set(true);
    }
}
