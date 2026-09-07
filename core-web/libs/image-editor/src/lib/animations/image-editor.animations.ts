import {
    animate,
    AnimationTriggerMetadata,
    keyframes,
    style,
    transition,
    trigger
} from '@angular/animations';

/**
 * Scale-and-fade entrance/exit for the image editor modal.
 * @param duration - Animation duration (default: 200ms; pass '0ms' for reduced motion)
 * @param easing - CSS easing function (default: 'ease-out')
 */
export const imageEditorModalScaleFade = (
    duration = '200ms',
    easing = 'ease-out'
): AnimationTriggerMetadata => {
    return trigger('imageEditorModalScaleFade', [
        transition(':enter', [
            style({ opacity: 0, transform: 'scale(0.96)' }),
            animate(`${duration} ${easing}`, style({ opacity: 1, transform: 'scale(1)' }))
        ]),
        transition(':leave', [
            animate(`${duration} ${easing}`, style({ opacity: 0, transform: 'scale(0.96)' }))
        ])
    ]);
};

/**
 * Crossfade between two preview images as filters are applied.
 * @param duration - Animation duration (default: 150ms; pass '0ms' for reduced motion)
 * @param easing - CSS easing function (default: 'ease-in-out')
 */
export const imageCrossfade = (
    duration = '150ms',
    easing = 'ease-in-out'
): AnimationTriggerMetadata => {
    return trigger('imageCrossfade', [
        transition(':enter', [
            style({ opacity: 0 }),
            animate(`${duration} ${easing}`, style({ opacity: 1 }))
        ]),
        transition(':leave', [animate(`${duration} ${easing}`, style({ opacity: 0 }))])
    ]);
};

/**
 * Fade for transient overlays (loaders, toolbars) entering and leaving.
 * @param duration - Animation duration (default: 150ms; pass '0ms' for reduced motion)
 * @param easing - CSS easing function (default: 'ease-in-out')
 */
export const imageEditorOverlayEnterLeave = (
    duration = '150ms',
    easing = 'ease-in-out'
): AnimationTriggerMetadata => {
    return trigger('imageEditorOverlayEnterLeave', [
        transition(':enter', [
            style({ opacity: 0 }),
            animate(`${duration} ${easing}`, style({ opacity: 1 }))
        ]),
        transition(':leave', [animate(`${duration} ${easing}`, style({ opacity: 0 }))])
    ]);
};

/**
 * Brief pulse used to acknowledge a successful save.
 * @param duration - Animation duration (default: 400ms; pass '0ms' for reduced motion)
 * @param easing - CSS easing function (default: 'ease-out')
 */
export const saveSuccessPulse = (
    duration = '400ms',
    easing = 'ease-out'
): AnimationTriggerMetadata => {
    return trigger('saveSuccessPulse', [
        transition('* => pulse', [
            animate(
                `${duration} ${easing}`,
                keyframes([
                    style({ transform: 'scale(1)', offset: 0 }),
                    style({ transform: 'scale(1.08)', offset: 0.5 }),
                    style({ transform: 'scale(1)', offset: 1 })
                ])
            )
        ])
    ]);
};
