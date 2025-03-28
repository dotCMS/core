/* eslint-disable @typescript-eslint/no-explicit-any */
import { createUVESubscription } from '../lib/core/core.utils';
import { computeScrollIsInBottom } from '../lib/dom/dom.utils';
import { setBounds } from '../lib/editor/internal';
import { sendMessageToEditor } from '../lib/editor/public';
import { DotCMSUVEAction, UVEEventType } from '../lib/types/editor/public';

export function scrollHandler(): void {
    const scrollCallback = () => {
        sendMessageToEditor({
            action: DotCMSUVEAction.IFRAME_SCROLL
        });
    };

    const scrollEndCallback = () => {
        sendMessageToEditor({
            action: DotCMSUVEAction.IFRAME_SCROLL_END
        });
    };

    window.addEventListener('scroll', scrollCallback);
    window.addEventListener('scrollend', scrollEndCallback);
}

export function addClassToEmptyContentlets(): void {
    const contentlets = document.querySelectorAll('[data-dot-object="contentlet"]');

    contentlets.forEach((contentlet) => {
        if (contentlet.clientHeight) {
            return;
        }

        contentlet.classList.add('empty-contentlet');
    });
}

export function registerUVEEvents() {
    createUVESubscription(UVEEventType.PAGE_RELOAD, () => {
        window.location.reload();
    });

    createUVESubscription(UVEEventType.REQUEST_BOUNDS, (bounds) => {
        setBounds(bounds);
    });

    createUVESubscription(UVEEventType.IFRAME_SCROLL, (direction) => {
        if (
            (window.scrollY === 0 && direction === 'up') ||
            (computeScrollIsInBottom() && direction === 'down')
        ) {
            // If the iframe scroll is at the top or bottom, do not send anything.
            // This avoids losing the scrollend event.
            return;
        }

        const scrollY = direction === 'up' ? -120 : 120;
        window.scrollBy({ left: 0, top: scrollY, behavior: 'smooth' });
    });

    createUVESubscription(UVEEventType.CONTENTLET_HOVERED, (contentletHovered) => {
        sendMessageToEditor({
            action: DotCMSUVEAction.SET_CONTENTLET,
            payload: contentletHovered
        });
    });
}

export function setClientIsReady(): void {
    sendMessageToEditor({
        action: DotCMSUVEAction.CLIENT_READY
    });
}
