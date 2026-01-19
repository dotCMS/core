import { Directive, effect, ElementRef, inject, input, OnDestroy, Renderer2 } from '@angular/core';

/**
 * Directive that animates a number counting up from 0 to the target value.
 *
 * @example
 * ```html
 * <span [dotCountUp]="45230" [duration]="1000"></span>
 * ```
 */
@Directive({
    selector: '[dotCountUp]',
    standalone: true
})
export class DotCountUpDirective implements OnDestroy {
    readonly #el = inject(ElementRef);
    readonly #renderer = inject(Renderer2);

    /** Target value to count up to */
    readonly $value = input.required<number>({ alias: 'dotCountUp' });

    /** Animation duration in milliseconds */
    readonly $duration = input<number>(1000, { alias: 'duration' });

    /** Easing function type */
    readonly $easing = input<'linear' | 'easeOut' | 'easeInOut'>('easeOut', { alias: 'easing' });

    /** Suffix to append after the number (e.g., '%', 'ms') */
    readonly $suffix = input<string>('', { alias: 'suffix' });

    /** Format type: 'number' for regular numbers, 'time' for seconds → "Xm Ys" format */
    readonly $format = input<'number' | 'time'>('number', { alias: 'format' });

    #animationId: number | null = null;

    constructor() {
        effect(() => {
            const value = this.$value();
            const duration = this.$duration();
            const easing = this.$easing();
            const suffix = this.$suffix();
            const format = this.$format();

            this.#animate(value, duration, easing, suffix, format);
        });
    }

    ngOnDestroy(): void {
        if (this.#animationId) {
            cancelAnimationFrame(this.#animationId);
        }
    }

    #animate(
        targetValue: number,
        duration: number,
        easing: string,
        suffix: string,
        format: 'number' | 'time'
    ): void {
        // Cancel any existing animation
        if (this.#animationId) {
            cancelAnimationFrame(this.#animationId);
        }

        // If duration is 0 or very small, skip animation
        if (duration <= 0) {
            this.#renderer.setProperty(
                this.#el.nativeElement,
                'textContent',
                this.#formatValue(targetValue, suffix, format)
            );

            return;
        }

        const startTime = performance.now();
        const startValue = 0;

        const step = (currentTime: number) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            // Apply easing
            const easedProgress = this.#applyEasing(progress, easing);

            // Calculate current value
            const currentValue = Math.round(
                startValue + (targetValue - startValue) * easedProgress
            );

            // Update DOM
            this.#renderer.setProperty(
                this.#el.nativeElement,
                'textContent',
                this.#formatValue(currentValue, suffix, format)
            );

            if (progress < 1) {
                this.#animationId = requestAnimationFrame(step);
            }
        };

        this.#animationId = requestAnimationFrame(step);
    }

    #formatValue(value: number, suffix: string, format: 'number' | 'time'): string {
        if (format === 'time') {
            return this.#formatTime(value);
        }

        return value.toLocaleString() + suffix;
    }

    #formatTime(totalSeconds: number): string {
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;

        if (minutes === 0) {
            return `${seconds}s`;
        }

        return `${minutes}m ${seconds}s`;
    }

    #applyEasing(t: number, easing: string): number {
        switch (easing) {
            case 'linear':
                return t;
            case 'easeInOut':
                return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            case 'easeOut':
            default:
                return 1 - Math.pow(1 - t, 3); // easeOutCubic
        }
    }
}
