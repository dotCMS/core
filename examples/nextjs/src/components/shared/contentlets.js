'use client';
import React from 'react';
import Image from 'next/image';
import { editContentlet } from '@dotcms/client';

const dateFormatOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
};

function Contentlets({ contentlets }) {
    return (
        <ul className="flex flex-col gap-4">
            {contentlets.map((contentlet) => (
                <li className="relative flex gap-7 min-h-16" key={contentlet.identifier}>
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
                            className="text-sm font-bold text-zinc-900"
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
