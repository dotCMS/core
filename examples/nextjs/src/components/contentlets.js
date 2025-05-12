"use client";

import Image from "next/image";

import { editContentlet } from "@dotcms/client";
import { useIsEditMode } from "@/hooks/isEditMode";

const dateFormatOptions = {
    year: "numeric",
    month: "long",
    day: "numeric",
};

function Contentlets({ contentlets }) {
    const isEditMode = useIsEditMode();

    return (
        <ul className="flex flex-col gap-7">
            {contentlets.map(
                ({
                    url,
                    title,
                    inode,
                    image,
                    urlMap,
                    modDate,
                    urlTitle,
                    identifier
                }) => (
                    <li
                        className="flex gap-7 min-h-16 relative"
                        key={identifier}
                    >
                        {isEditMode && (
                            <button
                                onClick={() => editContentlet(contentlet)}
                                style={{
                                    color: "black",
                                    border: "1px solid black",
                                    position: "absolute",
                                    bottom: 0,
                                    right: 0,
                                    width: "40px",
                                    height: "20px",
                                    zIndex: 1000,
                                    display: "flex",
                                    justifyContent: "center",
                                    alignItems: "center",
                                }}
                            >
                                Edit
                            </button>
                        )}
                        <a className="relative min-w-32" href={urlMap || url}>
                            {image && (
                                <Image
                                    src={inode}
                                    alt={urlTitle}
                                    fill={true}
                                    className="object-cover"
                                />
                            )}
                        </a>
                        <div className="flex flex-col gap-1">
                            <a
                                className="text-sm text-white font-bold"
                                href={urlMap || url}
                            >
                                {title}
                            </a>
                            <time className="text-gray-400">
                                {new Date(modDate).toLocaleDateString(
                                    "en-US",
                                    dateFormatOptions
                                )}
                            </time>
                        </div>
                    </li>
                )
            )}
        </ul>
    );
}

export default Contentlets;
