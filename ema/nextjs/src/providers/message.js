'use client';

import React, { createContext } from 'react';
import usePostMessage from '@/hooks/usePostMessage';

export const PostMessageContext = createContext({});

/**
 * Provider to handle postMessage, this way we centralize the messages and have access to the same method in all components
 *
 * @export
 * @param {*} { children }
 * @return {*}
 */
export default function PostMessageProvider({ children }) {
    const { postMessage } = usePostMessage();

    return (
        <PostMessageContext.Provider value={{ postMessage }}>
            {children}
        </PostMessageContext.Provider>
    );
}
