"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

import { enableBlockEditorInline } from "@dotcms/uve";
import { DotCMSBlockEditorRenderer, useEditableDotCMSPage } from "@dotcms/react/next";

import { useIsEditMode } from "@/hooks/isEditMode";
import Footer from "@/components/footer/footer";
import Header from "@/components/header";

export function DetailPage({ pageContent }) {
    const [twActives, setTwActives] = useState(
        "prose lg:prose-xl prose-a:text-blue-600"
    );
    const { pageAsset, content } = useEditableDotCMSPage(pageContent);
    const { urlContentMap } = pageAsset;
    const { blogContent } = urlContentMap || {};
    const isEditMode = useIsEditMode();

    console.log("HERE - DETAIL PAGE");

    useEffect(() => {
        if (isEditMode) {
            setTwActives((prev) => {
                return `${prev} border-2 border-solid border-cyan-400 cursor-pointer`;
            });
        }
    }, [isEditMode]);

    const handleClick = () => {
        if (isEditMode) {
            enableBlockEditorInline(urlContentMap, "blogContent");
        }
    };

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            {pageAsset?.layout.header && <Header />}
            <main className="flex flex-col gap-8 m-auto">
                {urlContentMap?.image && (
                    <div className="relative w-full h-80 overflow-hidden">
                        <Image
                            className="object-cover"
                            src={urlContentMap.inode}
                            fill={true}
                            alt="Activity Image"
                        />
                    </div>
                )}

                <div onClick={handleClick}>
                    <DotCMSBlockEditorRenderer
                        blocks={JSON.parse(blogContent)}
                        className={twActives}
                        customRenderers={customeRenderers}
                    />
                </div>
            </main>

            {pageAsset?.layout.footer && <Footer {...content} />}
        </div>
    );
}

const customeRenderers = {
    Activity: (props) => {
        const { title, description } = props.attrs.data;

        return (
            <div>
                <h1>{title}</h1>
                <p>{description}</p>
            </div>
        );
    },
};
