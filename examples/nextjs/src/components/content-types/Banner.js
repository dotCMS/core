"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect } from "react";

import { DotCMSEditableText } from "@dotcms/react";
import { registerComponentStyleConfiguration } from "@dotcms/uve";

function Banner(contentlet) {
    const { title, caption, inode, image, link, buttonText } = contentlet;

    useEffect(() => {
        registerComponentStyleConfiguration("Banner", {
            title: {
                color: {
                    type: "single select",
                    options: ["#000000", "#FFFFFF", "#000000", "#FFFFFF"]
                },
            },
        });
    }, []);

    return (
        <div className="relative w-full p-4 bg-gray-200 h-96">
            {image && (
                <Image
                    src={inode}
                    fill={true}
                    className="object-cover"
                    alt={title}
                />
            )}
            <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white">
                <h2 className="mb-2 text-6xl font-bold text-shadow">
                    <DotCMSEditableText
                        contentlet={contentlet}
                        fieldName="title"
                    />
                </h2>
                <p className="mb-4 text-xl text-shadow">{caption}</p>
                {link && (
                    <Link
                        className="p-4 text-xl transition duration-300 bg-blue-500 rounded-sm hover:bg-blue-700"
                        href={link}
                    >
                        {buttonText || "See more"}
                    </Link>
                )}
            </div>
        </div>
    );
}

export default Banner;
