'use client';

import React, { useEffect } from 'react';
import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';
import { usePathname } from 'next/navigation';
import Header from './layout/header';
import Footer from './layout/footer';
import Row from '@/lib/components/row';

function reloadWindow(event) {
    if (event.data !== 'ema-reload-page') return;

    window.location.reload();
}

// Main layout component
export const DotcmsPage = () => {
    const pathname = usePathname();
    // Get the page layout from the global context
    const { layout, page } = useContext(GlobalContext);

    useEffect(() => {
        const url = pathname.split('/');

        window.parent.postMessage(
            {
                action: 'set-url',
                payload: {
                    url: url === '/' ? 'index' : url.pop() //TODO: We need to enhance this, this will break for: nested/pages/like/this
                }
            },
            '*'
        );
    }, [pathname]);

    useEffect(() => {
        window.addEventListener('message', reloadWindow);

        return () => {
            window.removeEventListener('message', reloadWindow);
        };
    }, []);

    return (
        <div className="flex flex-col min-h-screen gap-6">
            {layout.header && <Header />}
            <main className="container flex flex-col gap-8 m-auto">
                <h1 className="text-xl font-bold">{page.title}</h1>
                {layout.body.rows.map((row, index) => (
                    <Row key={index} row={row} />
                ))}
            </main>
            {layout.footer && <Footer />}
        </div>
    );
};
