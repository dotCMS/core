'use client';

import React, { useCallback, useEffect, useRef } from 'react';
import { useContext } from 'react';
import { usePathname } from 'next/navigation';
import { useRouter } from 'next/navigation';

import { GlobalContext } from '@/lib/providers/global';
import { getPageElementBound } from '@/lib/utils';
import Row from '@/lib/components/row';

import Header from './layout/header';
import Footer from './layout/footer';

// Main layout component
export const DotcmsPage = () => {
    const rowsRef = useRef([]);

    const pathname = usePathname();
    const router = useRouter();

    const { layout, page } = useContext(GlobalContext);

    useEffect(() => {
        window.parent.postMessage(
            {
                action: 'set-url',
                payload: {
                    url: pathname === '/' ? 'index' : pathname.replace('/', '')
                }
            },
            '*'
        );
    }, [pathname]);

    // useCallBack to avoid re-create on every render
    const eventMessageHandler = useCallback(
        (event) => {
            switch (event.data) {
                case 'ema-reload-page':
                    router.refresh();
                    break;
                case 'ema-request-bounds':
                    const positionData = getPageElementBound(rowsRef.current);

                    window.parent.postMessage(
                        {
                            action: 'set-bounds',
                            payload: positionData
                        },
                        '*'
                    );
                    break;
                default:
                    break;
            }
        },
        [router]
    );
    // We need to unbound this from the component, with a custom hook maybe?

    const eventScrollHandler = useCallback((_event) => {
        window.parent.postMessage(
            {
                action: 'scroll'
            },
            '*'
        );
    }, []);

    useEffect(() => {
        window.addEventListener('message', eventMessageHandler);
        window.addEventListener('scroll', eventScrollHandler);

        return () => {
            window.removeEventListener('message', eventMessageHandler);
        };
    }, [eventMessageHandler]);

    const addRowRef = (el) => {
        if (el && !rowsRef.current.includes(el)) {
            rowsRef.current.push(el);
        }
    };

    return (
        <div className="flex flex-col min-h-screen gap-6">
            {layout.header && <Header />}
            <main className="container flex flex-col gap-8 m-auto">
                <h1 className="text-xl font-bold">{page.title}</h1>
                {layout.body.rows.map((row, index) => (
                    <Row ref={addRowRef} key={index} row={row} />
                ))}
            </main>
            {layout.footer && <Footer />}
        </div>
    );
};
