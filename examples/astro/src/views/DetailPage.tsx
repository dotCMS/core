import { useEffect, useState } from "react";

import type { BlockEditorNode } from "@dotcms/types";
import { enableBlockEditorInline } from "@dotcms/uve";
import {
    DotCMSBlockEditorRenderer,
    useEditableDotCMSPage,
} from "@dotcms/react";

import type { DotCMSCustomDetailPageResponse } from "@/types/page.model";

import Footer from "@/components/common/Footer";
import Header from "@/components/common/Header";
import { useIsEditMode } from "@/hooks";

export function DetailPage({ pageResponse }: { pageResponse: DotCMSCustomDetailPageResponse }) {
    const [blockEditorClasses, setBlockEditorClasses] = useState(
        "prose lg:prose-xl prose-a:text-blue-600",
    );

    const { pageAsset, content } = useEditableDotCMSPage<DotCMSCustomDetailPageResponse>(pageResponse);
    const { urlContentMap, layout } = pageAsset;
    const { blogContent } = urlContentMap || {};

    const showHeader = layout.header && content;
    const showFooter = layout.footer && content;
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
            enableBlockEditorInline(urlContentMap as any, "blogContent");
        }
    };

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            {showHeader && <Header navigation={content?.navigation} />}
            <main className="flex flex-col gap-8 m-auto">
                {urlContentMap?.image && (
                    <div className="relative w-full h-80 overflow-hidden">
                        <img src={`/dA/${urlContentMap.inode}`} className="w-full h-full object-cover" alt="Activity Image" />
                    </div>
                )}

                <div onClick={handleClick}>
                    <DotCMSBlockEditorRenderer
                        blocks={JSON.parse(blogContent || "[]")}
                        className={blockEditorClasses}
                        customRenderers={customeRenderers}
                    />
                </div>
            </main>

            {showFooter && <Footer {...content} />}
        </div>
    );
}

const customeRenderers = {
    Activity: (props: BlockEditorNode) => {
        const { title, description } = props.attrs?.data || {};

        return (
            <div>
                <h1>{title}</h1>
                <p>{description}</p>
            </div>
        );
    },
    Product: (props: BlockEditorNode) => {
        const { title, description } = props.attrs?.data || {};

        return (
            <div>
                <h1>{title}</h1>
                <div dangerouslySetInnerHTML={{ __html: description }} />
            </div>
        );
    },
};
