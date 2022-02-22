import { Component, OnInit } from '@angular/core';
import { PrimeNGConfig } from 'primeng/api';

@Component({
    selector: 'dot-p-button-ripple',
    template: `<button
        type="button"
        pButton
        pRipple
        label="Success"
        class="p-button-success"
    ></button>`
})
export class RippleComponent implements OnInit {
    constructor(private primengConfig: PrimeNGConfig) {}

    ngOnInit() {
        this.primengConfig.ripple = true;
    }
}
