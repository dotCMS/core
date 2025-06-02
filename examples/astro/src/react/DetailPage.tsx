import { useEffect, useState } from "react";

import { enableBlockEditorInline } from "@dotcms/uve";
import {
    DotCMSBlockEditorRenderer,
    useEditableDotCMSPage,
} from "@dotcms/react/next";

import Footer from "@components/react/Footer";
import Header from "@components/react/Header";
import { useIsEditMode } from "@react/hooks/isEditMode";

export function DetailPage({ pageResponse }: { pageResponse: any }) {
    const [blockEditorClasses, setBlockEditorClasses] = useState(
        "prose lg:prose-xl prose-a:text-blue-600",
    );

    console.log(pageResponse);
    const { pageAsset, content } = useEditableDotCMSPage<any>(pageResponse);
    const { urlContentMap } = pageAsset;
    const { blogContent } = urlContentMap as any || {};
    const navigation = content?.navigation as any;
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
            {pageAsset?.layout.header && (
                <Header navItems={navigation?.children} />
            )}
            <main className="flex flex-col gap-8 m-auto">
                {urlContentMap?.image && (
                    <div className="relative w-full h-80 overflow-hidden">
                        <img src={`/dA/${urlContentMap.inode}`} className="w-full h-full object-cover" alt="Activity Image" />
                    </div>
                )}

                <div onClick={handleClick}>
                    <DotCMSBlockEditorRenderer
                        blocks={JSON.parse(blogContent)}
                        className={blockEditorClasses}
                        customRenderers={customeRenderers}
                    />
                </div>
            </main>

            {pageAsset?.layout.footer && <Footer {...content} />}
        </div>
    );
}

const customeRenderers = {
    Activity: (props: any) => {
        const { title, description } = props.attrs.data;

        return (
            <div>
                <h1>{title}</h1>
                <p>{description}</p>
            </div>
        );
    },
    Product: (props: any) => {
        const { title, description } = props.attrs.data;

        return (
            <div>
                <h1>{title}</h1>
                <div dangerouslySetInnerHTML={{ __html: description }} />
            </div>
        );
    },
};
