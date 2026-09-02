import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';

import { PrimeNGConfig } from 'primeng/api';

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
    private primengConfig = inject(PrimeNGConfig);

    ngOnInit() {
        this.primengConfig.ripple = true;
    }
}
