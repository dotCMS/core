import React, { useLayoutEffect } from 'react';

function reloadWindow(event) {
    if (event.data !== 'reload') return;

    window.location.reload();
}

/**
 * Hook to handle postMessage
 *
 * @return {*}
 */
const usePostMessage = () => {
    useLayoutEffect(() => {
        window.addEventListener('message', reloadWindow);

        return () => {
            window.removeEventListener('message', reloadWindow);
        };
    }, []);

    return {
        postMessage: (message) => {
            window.parent.postMessage(message, '*');

            console.log('message sent', message);
        }
    };
};

export default usePostMessage;
