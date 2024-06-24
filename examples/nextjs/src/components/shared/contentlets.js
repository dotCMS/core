import React from "react";
import Image from "next/image";

const dateFormatOptions = {
    year: "numeric",
    month: "long",
    day: "numeric",
};

function Contentlets({ contentlets }) {
    return (
        <ul className="flex flex-col gap-7">
            {contentlets.map((contentlet) => (
                <li key={contentlet.identifier} className="flex gap-7 min-h-16">
                    <a
                        className="relative min-w-32"
                        href={contentlet.urlMap || contentlet.url}
                    >
                        {contentlet.image && (
                            <Image
                                src={
                                    contentlet.image?.idPath ?? contentlet.image
                                }
                                alt={contentlet.urlTitle}
                                fill={true}
                                className="object-cover"
                            />
                        )}
                    </a>
                    <div className="flex flex-col gap-1">
                        <a
                            className="text-sm text-zinc-900 font-bold"
                            href={contentlet.urlMap || contentlet.url}
                        >
                            {contentlet.title}
                        </a>
                        <time className="text-zinc-600">
                            {new Date(contentlet.modDate).toLocaleDateString(
                                "en-US",
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
