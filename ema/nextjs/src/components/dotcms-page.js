'use client';

import React, { useEffect } from 'react';
import { useContext } from 'react';
import { GlobalContext } from '../providers/global';
import Image from 'next/image';
import Link from 'next/link';
import { getPageContainers } from '@/utils';
import { usePathname } from 'next/navigation';

// Provide a component for each content type
const contentComponents = {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent
};

function WebPageContent({ title, body }) {
    return (
        <>
            <h1 className="text-xl font-bold">{title}</h1>
            <div dangerouslySetInnerHTML={{ __html: body }} />
        </>
    );
}

function ImageComponent({ fileAsset, title, description }) {
    const {
        viewAs: { language }
    } = useContext(GlobalContext);

    return (
        <div className="relative overflow-hidden bg-white rounded shadow-lg group">
            <div className="relative w-full bg-gray-200 h-96">
                <Image
                    src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${fileAsset}?language_id=${language.id}`}
                    fill={true}
                    className="object-cover"
                    alt={title}
                />
            </div>
            <div className="absolute bottom-0 w-full px-6 py-8 text-white transition-transform duration-300 translate-y-full bg-orange-500 bg-opacity-80 w-100 group-hover:translate-y-0">
                <div className="mb-2 text-2xl font-bold">{title}</div>
                <p className="text-base">{description}</p>
            </div>
        </div>
    );
}

function Activity({ title, description, image, urlTitle }) {
    const {
        viewAs: { language }
    } = useContext(GlobalContext);

    return (
        <article className="p-4 overflow-hidden bg-white rounded shadow-lg">
            <Image
                className="w-full"
                src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${image}?language_id=${language.id}`}
                width={100}
                height={100}
                alt="Activity Image"
            />
            <div className="px-6 py-4">
                <p className="mb-2 text-xl font-bold">{title}</p>
                <p className="text-base line-clamp-3">{description}</p>
            </div>
            <div className="px-6 pt-4 pb-2">
                <Link
                    href={`/activities/${urlTitle || '#'}`}
                    className="inline-block px-4 py-2 font-bold text-white bg-blue-500 rounded-full hover:bg-blue-700">
                    Link to detail →
                </Link>
            </div>
        </article>
    );
}

function Banner({ title, image, caption, buttonText, link }) {
    const {
        viewAs: { language }
    } = useContext(GlobalContext);

    return (
        <div className="relative w-full p-4 bg-gray-200 h-96">
            <Image
                src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${image}?language_id=${language.id}`}
                fill={true}
                className="object-cover"
                alt={title}
            />
            <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white">
                <h2 className="mb-2 text-6xl font-bold text-shadow">{title}</h2>
                <p className="mb-4 text-xl text-shadow">{caption}</p>
                <Link
                    className="p-4 text-xl transition duration-300 bg-blue-500 rounded hover:bg-blue-600"
                    href={link || '#'}>
                    {buttonText}
                </Link>
            </div>
        </div>
    );
}

function Product({ image, title, salePrice, retailPrice, urlTitle }) {
    const {
        viewAs: { language }
    } = useContext(GlobalContext);

    const formatPrice = (price) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(price);
    };

    return (
        <div className="overflow-hidden bg-white rounded shadow-lg">
            <div className="p-4">
                <Image
                    className="w-full"
                    src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${image}?language_id=${language.id}`}
                    width={100}
                    height={100}
                    alt="Activity Image"
                />
            </div>
            <div className="px-6 py-4 bg-slate-100">
                <div className="mb-2 text-xl font-bold">{title}</div>
                {retailPrice && salePrice ? (
                    <>
                        <div className="text-gray-500 line-through">{formatPrice(retailPrice)}</div>
                        <div className="text-3xl font-bold ">{formatPrice(salePrice)}</div>
                    </>
                ) : (
                    <div className="text-3xl font-bold">
                        {retailPrice ? formatPrice(retailPrice) : formatPrice(salePrice)}
                    </div>
                )}
                <Link
                    href={`/store/products/${urlTitle || '#'}`}
                    className="inline-block px-4 py-2 mt-4 text-white bg-green-500 rounded hover:bg-green-600">
                    Buy Now
                </Link>
            </div>
        </div>
    );
}

function NoContent({ contentType }) {
    return <h1>No Content for {contentType}</h1>;
}

// Header component
const Header = () => {
    const { nav } = useContext(GlobalContext);
    return (
        <header className="flex items-center justify-between p-4 bg-blue-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux</Link>
                </h2>
            </div>
            <Navigation className="text-white" nav={nav} />
            <div className="flex items-center">
                <select className="px-2 py-1 border border-gray-300 rounded">
                    <option value="en">English</option>
                    <option value="es">Español</option>
                    <option value="fr">Français</option>
                </select>
            </div>
        </header>
    );
};

// Button component
function ActionButton({ message, children }) {
    return (
        <button
            style={{
                border: 0,
                backgroundColor: 'lightgray',
                color: 'black',
                border: 'solid 1px',
                padding: '0.25rem 0.5rem',
                marginBottom: '0.5rem'
            }}
            onClick={() => {
                window.parent.postMessage(message, '*');
            }}>
            {children || message.action}
        </button>
    );
}

// Footer component
const Footer = () => {
    return <footer className="p-4 text-white bg-blue-500">Footer</footer>;
};

// Container component
const Container = ({ containerRef }) => {
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, page } = useContext(GlobalContext);

    const { container, containerStructures } = containers[identifier];

    const { inode, maxContentlets } = container;

    // Get accepts types of content types for this container
    const acceptTypes = containerStructures.map((structure) => structure.contentTypeVar).join(',');

    // Get the contentlets for "this" container
    const contentlets = containers[identifier].contentlets[`uuid-${uuid}`];

    // Memoize the contenlets to avoid re-rendering
    const contentletsId = contentlets.map((contentlet) => contentlet.identifier);

    // Memoize the page containers to avoid re-rendering
    const pageContainers = getPageContainers(containers);

    return (
        <>
            <ActionButton
                message={{
                    action: 'add-contentlet',
                    payload: {
                        pageID: page.identifier,
                        container: {
                            identifier: container.path ?? container.identifier,
                            uuid,
                            contentletsId,
                            acceptTypes
                        },
                        pageContainers
                    }
                }}>
                +
            </ActionButton>
            <div
                className="flex flex-col gap-4"
                data-dot-accept-types={acceptTypes}
                data-dot-object="container"
                data-dot-inode={inode}
                data-dot-identifier={identifier}
                data-dot-uuid={uuid}
                data-max-contentlets={maxContentlets}
                data-dot-can-add="CONTENT,FORM,WIDGET">
                {contentlets.map((contentlet) => {
                    const {
                        identifier,
                        inode,
                        contentType,
                        baseType,
                        title,
                        languageId,
                        dotContentTypeId
                    } = contentlet;

                    const Component = contentComponents[contentlet.contentType] || NoContent;

                    return (
                        <div
                            className="p-4 border border-gray-300"
                            key={contentlet.identifier}
                            data-dot-object="contentlet"
                            data-dot-inode={inode}
                            data-dot-identifier={identifier}
                            data-dot-type={contentType}
                            data-dot-basetype={baseType}
                            data-dot-lang={languageId}
                            data-dot-title={title}
                            data-dot-can-edit={true}
                            data-dot-content-type-id={dotContentTypeId}
                            data-dot-has-page-lang-version="true">
                            <ActionButton
                                message={{
                                    action: 'edit-contentlet',
                                    payload: contentlet
                                }}>
                                Edit
                            </ActionButton>
                            <Component {...contentlet} />
                        </div>
                    );
                })}
            </div>
        </>
    );
};

// Column component
const Column = ({ column }) => {
    // Calculate Tailwind classes for grid column span and start
    const widthClassMap = {
        1: 'col-span-1',
        2: 'col-span-2',
        3: 'col-span-3',
        4: 'col-span-4',
        5: 'col-span-5',
        6: 'col-span-6',
        7: 'col-span-7',
        8: 'col-span-8',
        9: 'col-span-9',
        10: 'col-span-10',
        11: 'col-span-11',
        12: 'col-span-12'
    };

    const statrClassMap = {
        1: 'col-start-1',
        2: 'col-start-2',
        3: 'col-start-3',
        4: 'col-start-4',
        5: 'col-start-5',
        6: 'col-start-6',
        7: 'col-start-7',
        8: 'col-start-8',
        9: 'col-start-9',
        10: 'col-start-10',
        11: 'col-start-11',
        12: 'col-start-12'
    };

    const widthClass = widthClassMap[column.width];
    const startClass = statrClassMap[column.leftOffset];

    return (
        <div className={`${widthClass} ${startClass}`}>
            {column.containers.map((container, index) => (
                <Container key={container.identifier} containerRef={container} />
            ))}
        </div>
    );
};

// Row component
const Row = ({ row }) => {
    return (
        <div className="grid grid-cols-12 gap-4">
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
};

function Navigation({ nav, className }) {
    return (
        <nav className={className}>
            <ul className="flex space-x-4">
                <li>
                    <Link href="/">Home</Link>
                </li>
                {nav.map((item) => (
                    <li key={item.folder}>
                        <Link href={item.href} target={item.target}>
                            {item.title}
                        </Link>
                    </li>
                ))}
            </ul>
        </nav>
    );
}

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
        window.parent.postMessage(
            {
                action: 'set-url',
                payload: {
                    url: url === '/' ? 'index' : pathname.split('/').pop()
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
