import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-hello-world',
    standalone: true,
    imports: [CommonModule],
    template: ` <p>hello-world works!</p> `,
    styles: [],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class HelloWorldComponent {}
