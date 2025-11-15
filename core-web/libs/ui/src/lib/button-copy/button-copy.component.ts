import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    inject
} from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

@Component({
    selector: 'dot-button-copy',
    imports: [CommonModule, TooltipModule],
    templateUrl: './button-copy.component.html',
    styleUrl: './button-copy.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true
})
export class ButtonCopyComponent implements OnDestroy {
    copied = false;
    private resetTimeoutId?: ReturnType<typeof setTimeout>;
    private readonly cdr = inject(ChangeDetectorRef);

    handleClick(): void {
        if (this.copied) {
            return;
        }

        this.copied = true;
        this.cdr.markForCheck();
        this.resetTimeoutId = setTimeout(() => {
            this.copied = false;
            this.cdr.markForCheck();
        }, 800);
    }

    ngOnDestroy(): void {
        if (this.resetTimeoutId) {
            clearTimeout(this.resetTimeoutId);
        }
    }
}
