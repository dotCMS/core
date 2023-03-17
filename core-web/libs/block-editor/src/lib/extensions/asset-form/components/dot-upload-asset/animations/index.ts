import { animate, keyframes, state, style, transition, trigger } from '@angular/animations';

export const shakeAnimation = trigger('shakeit', [
    state(
        'shakestart',
        style({
            transform: 'scale(1.1)'
        })
    ),
    state(
        'shakeend',
        style({
            transform: 'scale(1)'
        })
    ),
    transition(
        'shakestart => shakeend',
        animate(
            '1000ms ease-in',
            keyframes([
                style({ transform: 'translate3d(-2px, 0, 0)', offset: 0.1 }),
                style({ transform: 'translate3d(4px, 0, 0)', offset: 0.2 }),
                style({ transform: 'translate3d(-4px, 0, 0)', offset: 0.3 }),
                style({ transform: 'translate3d(4px, 0, 0)', offset: 0.4 }),
                style({ transform: 'translate3d(-4px, 0, 0)', offset: 0.5 }),
                style({ transform: 'translate3d(4px, 0, 0)', offset: 0.6 }),
                style({ transform: 'translate3d(-4px, 0, 0)', offset: 0.7 }),
                style({ transform: 'translate3d(4px, 0, 0)', offset: 0.8 }),
                style({ transform: 'translate3d(-2px, 0, 0)', offset: 0.9 })
            ])
        )
    )
]);
