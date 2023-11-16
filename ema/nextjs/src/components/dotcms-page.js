'use client';

import { useContext } from 'react';
import { GlobalContext } from '../providers/global';

function WebPageContent({ title, body }) {
    return (
        <>
            <h1 className="text-xl font-bold">{title}</h1>
            <div dangerouslySetInnerHTML={{ __html: body }} />
        </>
    );
}

function NoContent() {
    return <h1>No Content</h1>;
}

// Provide a component for each content type
const contentComponents = {
    webPageContent: WebPageContent
};

// Header component
const Header = () => {
    return <header className="p-4 text-white bg-blue-500">Header</header>;
};

// Footer component
const Footer = () => {
    return <footer className="p-4 text-white bg-blue-500">Footer</footer>;
};

// Container component
const Container = ({ containerRef }) => {
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers } = useContext(GlobalContext);

    const { container, containerStructures } = containers[identifier];
    const { inode, maxContentlets } = container;

    // Get accepts types of content types for this container
    const acceptTypes = containerStructures.map((structure) => structure.contentTypeVar).join(',');

    // Get the contentlets for "this" container
    const contentlets = containers[identifier].contentlets[`uuid-${uuid}`];

    return (
        <div
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

                // Get the component for the content type or use the NoContent component
                const Component = contentComponents[contentlet.contentType] || NoContent;

                return (
                    <div
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
                        <div className="p-4 border border-gray-300">
                            <Component {...contentlet} />
                        </div>
                    </div>
                );
            })}
        </div>
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
                <Container key={index} containerRef={container} />
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

// Main layout component
export const DotcmsPage = () => {
    // Get the page layout from the global context
    const { layout, page } = useContext(GlobalContext);

    return (
        <div className="flex flex-col min-h-screen">
            {layout.header && <Header />}
            <main className="flex-grow">
                <h1 className="text-xl font-bold">{page.title}</h1>
                {layout.body.rows.map((row, index) => (
                    <Row key={index} row={row} />
                ))}
            </main>
            {layout.footer && <Footer />}
        </div>
    );
};
