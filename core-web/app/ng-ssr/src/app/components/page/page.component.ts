import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal, effect } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'app-page',
    imports: [CommonModule],
    templateUrl: './page.component.html',
    styleUrl: './page.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageComponent {
    private router = inject(Router);

    currentRoute = signal(this.router.url);

    constructor() {
        effect(() => {
            this.currentRoute.set(this.router.url);
        });
    }
}
