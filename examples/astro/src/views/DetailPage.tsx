import { useEffect, useState } from "react";

import { enableBlockEditorInline } from "@dotcms/uve";
import {
  DotCMSBlockEditorRenderer,
  useEditableDotCMSPage,
  type CustomRendererProps,
} from "@dotcms/react";

import type { DotCMSCustomDetailPageResponse } from "@/types/page.model";

import Footer from "@/components/common/Footer";
import Header from "@/components/common/header/Header";
import { useIsEditMode } from "@/hooks";

export function DetailPage({
  pageResponse,
}: {
  pageResponse: DotCMSCustomDetailPageResponse;
}) {
  const [blockEditorClasses, setBlockEditorClasses] = useState(
    "prose lg:prose-xl prose-a:text-blue-600",
  );

  const { pageAsset, content } =
    useEditableDotCMSPage<DotCMSCustomDetailPageResponse>(pageResponse);
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
            <img
              src={`/dA/${urlContentMap.inode}`}
              className="w-full h-full object-cover"
              alt="Activity Image"
            />
          </div>
        )}

        <div onClick={handleClick}>
          <DotCMSBlockEditorRenderer
            blocks={blogContent!}
            className={blockEditorClasses}
            customRenderers={customRenderers}
          />
        </div>
      </main>

      {showFooter && <Footer {...content} />}
    </div>
  );
}

const customRenderers = {
  Activity: (props: CustomRendererProps) => {
    const { node } = props;
    const { title, description, contentType } = node.attrs?.data || {};

    return (
      <div className="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
        <h2 className="text-2xl font-bold">{title}</h2>
        <p className="line-clamp-2">{description}</p>
        <p className="text-sm text-cyan-700">{contentType}</p>
      </div>
    );
  },
  Product: (props: CustomRendererProps) => {
    const { title, description, contentType } = props.node.attrs?.data || {};

    return (
      <div className="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
        <h2 className="text-2xl font-bold">{title}</h2>
        <div
          className="line-clamp-2"
          dangerouslySetInnerHTML={{ __html: description }}
        />
        <p className="text-sm text-blue-500">{contentType}</p>
      </div>
    );
  },
  Destination: (props: CustomRendererProps) => {
    const { title, shortDescription, contentType } =
      props.node.attrs?.data || {};

    return (
      <div className="p-6 mb-4 rounded-2xl bg-white shadow-lg">
        <h2 className="text-2xl font-bold">{title}</h2>
        <p className="line-clamp-2">{shortDescription}</p>
        <p className="text-sm text-indigo-700">{contentType}</p>
      </div>
    );
  },
};
