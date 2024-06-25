"use client";
import React from "react";
import Image from "next/image";
import Contentlet from "./contentlet";

const dateFormatOptions = {
    year: "numeric",
    month: "long",
    day: "numeric",
};

function Contentlets({ contentlets }) {
    return (
        <ul className="flex flex-col gap-7">
            {contentlets.map((contentlet) => (
                <Contentlet contentlet={contentlet} key={contentlet.identifier}>
                    <li className="flex gap-7 min-h-16">
                        <a
                            className="relative min-w-32"
                            href={contentlet.urlMap || contentlet.url}
                        >
                            <Image
                                src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${
                                    contentlet.image
                                }?language_id=${contentlet.languageId || 1}`}
                                alt={contentlet.urlTitle}
                                fill={true}
                                className="object-cover"
                            />
                        </a>
                        <div className="flex flex-col gap-1">
                            <a
                                className="text-sm text-zinc-900 font-bold"
                                href={contentlet.urlMap || contentlet.url}
                            >
                                {contentlet.title}
                            </a>
                            <time className="text-zinc-600">
                                {new Date(
                                    contentlet.modDate
                                ).toLocaleDateString(
                                    "en-US",
                                    dateFormatOptions
                                )}
                            </time>
                        </div>
                    </li>
                </Contentlet>
            ))}
        </ul>
    );
}

export default Contentlets;
