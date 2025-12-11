"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

import { enableBlockEditorInline } from "@dotcms/uve";
import {
    DotCMSBlockEditorRenderer,
    DotCMSEditableText,
    useEditableDotCMSPage,
} from "@dotcms/react";

import { useIsEditMode } from "@/hooks/isEditMode";
import Footer from "@/components/footer/Footer";
import Header from "@/components/Header";

export function DetailPage({ pageContent }) {
    const [blockEditorClasses, setBlockEditorClasses] = useState(
        "prose lg:prose-xl prose-a:text-blue-600",
    );
    const { pageAsset, content } = useEditableDotCMSPage(pageContent);
    const { urlContentMap } = pageAsset;
    const { blogContent } = urlContentMap || {};
    const navigation = content.navigation;
    const isEditMode = useIsEditMode();

    useEffect(() => {
        if (isEditMode) {
            setBlockEditorClasses((prev) => {
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
            {pageAsset?.layout.header && (
                <Header navItems={navigation?.children} />
            )}
            <main className="flex flex-col gap-8 m-auto">
                <h1 className="text-4xl font-bold">
                    <DotCMSEditableText
                        contentlet={urlContentMap}
                        fieldName="title"
                    />
                </h1>

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
                        blocks={blogContent}
                        className={blockEditorClasses}
                        customRenderers={customRenderers}
                    />
                </div>
            </main>

            {pageAsset?.layout.footer && <Footer {...content} />}
        </div>
    );
}

const customRenderers = {
    Destination: (props) => {
        const { title, shortDescription, activities } = props.node.attrs?.data || {};

        return (
            <div className="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
                <h2 className="text-2xl font-bold">{title}</h2>
                <p className="line-clamp-2">{shortDescription}</p>
                <h3 className="text-lg font-semibold mb-4">Activities</h3>
                <ul>
                    {activities.map((activity) => (
                        <li key={activity.identifier}>{activity.title}</li>
                    ))}
                </ul>
            </div>
        );
    },
    Product: (props) => {
        const { title, description } = props.node.attrs?.data || {};

        return (
            <div className="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
                <h2 className="mb-3 text-xl font-bold text-slate-800 line-clamp-2">
                    {title}
                </h2>
                <div
                    className="text-sm text-slate-600 line-clamp-4"
                    dangerouslySetInnerHTML={{ __html: description }}
                />
            </div>
        );
    },
};
