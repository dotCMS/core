'use client';
import { useMemo } from 'react';
import Image from 'next/image';
import { editContentlet } from '@dotcms/client';
import { isEditMode } from '@/utils/isEditMode';

const dateFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
};

function Contentlets({ contentlets }) {
    const insideEditor = useMemo(isEditMode, []);

    return (
        <ul className="flex flex-col gap-7">
            {contentlets.map((contentlet) => (
                <li className="flex gap-7 min-h-16 relative" key={contentlet.identifier}>
                    {insideEditor && (
                        <button
                            onClick={() => editContentlet(contentlet)}
                            style={{
                                color: 'black',
                                border: '1px solid black',
                                position: 'absolute',
                                bottom: 0,
                                right: 0,
                                width: '40px',
                                height: '20px',
                                zIndex: 1000,
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }}>
                            Edit
                        </button>
                    )}
                    <a className="relative min-w-32" href={contentlet.urlMap || contentlet.url}>
                        {contentlet.image && (
                            <Image
                                src={contentlet.image?.idPath ?? contentlet.image}
                                alt={contentlet.urlTitle}
                                fill={true}
                                className="object-cover"
                            />
                        )}
                    </a>
                    <div className="flex flex-col gap-1">
                        <a
                            className="text-sm text-zinc-900 font-bold"
                            href={contentlet.urlMap || contentlet.url}>
                            {contentlet.title}
                        </a>
                        <time className="text-zinc-600">
                            {new Date(contentlet.modDate).toLocaleDateString(
                                'en-US',
                                dateFormatOptions
                            )}
                        </time>
                    </div>
                </li>
            ))}
        </ul>
    );
}

export default Contentlets;
