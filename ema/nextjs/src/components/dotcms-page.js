'use client';

import React, { useEffect, useRef } from 'react';
import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';
import { usePathname } from 'next/navigation';
import Header from './layout/header';
import Footer from './layout/footer';
import Row from '@/lib/components/row';

function getPageElementBound(rowsRef) {
    return rowsRef.current.map((row) => {
        const rowRect = row.getBoundingClientRect();
        const columns = row.children;

        return {
            x: rowRect.x,
            y: rowRect.y,
            width: rowRect.width,
            height: rowRect.height,
            columns: Array.from(columns).map((column) => {
                const columnRect = column.getBoundingClientRect();
                const containers = Array.from(column.querySelectorAll('[data-dot="container"]'));

                const columnX = columnRect.left - rowRect.left;
                const columnY = columnRect.top - rowRect.top;

                return {
                    x: columnX,
                    y: columnY,
                    width: columnRect.width,
                    height: columnRect.height,
                    containers: containers.map((container) => {
                        const containerRect = container.getBoundingClientRect();
                        const contentlets = Array.from(
                            container.querySelectorAll('[data-dot="contentlet"]')
                        );

                        return {
                            x: 0,
                            y: containerRect.y - rowRect.top,
                            width: containerRect.width,
                            height: containerRect.height,
                            contentlets: contentlets.map((contentlet) => {
                                const contentletRect = contentlet.getBoundingClientRect();

                                return {
                                    x: 0,
                                    y: contentletRect.y - containerRect.y,
                                    width: contentletRect.width,
                                    height: contentletRect.height
                                };
                            })
                        };
                    })
                };
            })
        };
    });
}

// Main layout component
export const DotcmsPage = () => {
    const rowsRef = useRef([]);

    const pathname = usePathname();
    const { layout, page } = useContext(GlobalContext);

    function handleParentEvents(event) {
        switch (event.data) {
            case 'ema-reload-page':
                window.location.reload();
                break;
            case 'ema-request-bounds':
                const positionData = getPageElementBound(rowsRef);
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
    }

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
        window.addEventListener('message', handleParentEvents);

        return () => {
            window.removeEventListener('message', handleParentEvents);
        };
    }, []);

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
