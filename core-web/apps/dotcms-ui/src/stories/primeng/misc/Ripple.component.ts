import { Component, OnInit, inject } from '@angular/core';

import { PrimeNGConfig } from 'primeng/api';

@Component({
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
