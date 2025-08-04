import { animate, AnimationTriggerMetadata, style, transition, trigger } from '@angular/animations';

/**
 * Fade in/out animation with customizable timing
 * @param duration - Animation duration in milliseconds (default: 250)
 * @param easing - CSS easing function (default: 'ease-in-out')
 */
export const fadeInOut = (duration = 250, easing = 'ease-in-out'): AnimationTriggerMetadata => {
    return trigger('fadeInOut', [
        transition(':enter', [
            style({ opacity: 0 }),
            animate(`${duration}ms ${easing}`, style({ opacity: 1 }))
        ]),
        transition(':leave', [animate(`${duration}ms ${easing}`, style({ opacity: 0 }))])
    ]);
};

/**
 * Simple fade in/out animation (pre-configured)
 */
export const fadeInContent = trigger('fadeInContent', [
    transition(':enter', [style({ opacity: 0 }), animate('150ms ease-in', style({ opacity: 1 }))]),
    transition(':leave', [animate('150ms ease-out', style({ opacity: 0 }))])
]);
