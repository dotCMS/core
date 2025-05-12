"use client";

import { useEffect, useState } from "react";

import { enableBlockEditorInline } from "@dotcms/uve";
import { DotCMSBlockEditorRenderer, useEditableDotCMSPage } from "@dotcms/react/next";

import { isEditMode } from "@/utils/isEditMode";
import Footer from "@/components/layout/footer/footer";
import Header from "@/components/layout/header/header";

export function DetailPage({ pageContent }) {
    const [twActives, setTwActives] = useState("");
    const { pageAsset } = useEditableDotCMSPage(pageContent);
    const { urlContentMap } = pageAsset;
    const { blogContent } = urlContentMap || {};

    useEffect(() => {
        if (isEditMode()) {
            setTwActives( "border-2 border-solid border-cyan-400 cursor-pointer");
        }
    }, []);

    const handleClick = () => {
        enableBlockEditorInline(urlContentMap, "blogContent");
    };

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            {pageAsset?.layout.header && <Header />}

            <main className="flex flex-col gap-8 m-auto" onClick={handleClick}>
                <DotCMSBlockEditorRenderer
                    blocks={JSON.parse(blogContent)}
                    className={twActives}
                    customRenderers={customeRenderers}
                />
            </main>

            {/* {pageAsset?.layout.footer && <Footer />} */}
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
    }
}
