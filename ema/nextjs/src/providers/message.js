'use client';

import React, { createContext } from 'react';

export const PostMessageContext = createContext({});

/**
 * Provider to handle postMessage, this way we centralize the messages and have access to the same method in all components
 *
 * @export
 * @param {*} { children, postMessage }
 * @return {*}
 */
export default function PostMessageProvider({ children, postMessage }) {
    return (
        <PostMessageContext.Provider value={postMessage}>{children}</PostMessageContext.Provider>
    );
}
