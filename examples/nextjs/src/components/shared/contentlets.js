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
                    <div className="relative min-w-32">
                        <Image
                            src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${
                                contentlet.image
                            }?language_id=${contentlet.languageId || 1}`}
                            alt={contentlet.urlTitle}
                            fill={true}
                            className="object-cover"
                        />
                    </div>
                    <div className="flex flex-col gap-1">
                        <a
                            className="text-sm text-yellow-200 font-bold"
                            href={contentlet.urlMap}
                        >
                            {contentlet.title}
                        </a>
                        <time>
                            {new Date(
                                contentlet.publishDate
                            ).toLocaleDateString("en-US", dateFormatOptions)}
                        </time>
                    </div>
                </li>
            ))}
        </ul>
    );
}

export default Contentlets;
