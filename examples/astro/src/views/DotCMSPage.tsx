import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";
import type { DotCMSCustomPageResponse } from "@/types/page.model";

import { dotComponents } from "@/components/content-types";
import Footer from "@/components/common/Footer";
import Header from "@/components/common/header/Header";

export const DotCMSPage = ({
  pageResponse,
}: {
  pageResponse: DotCMSCustomPageResponse;
}) => {
  const { pageAsset, content } =
    useEditableDotCMSPage<DotCMSCustomPageResponse>(pageResponse);
  const { layout } = pageAsset;

  const showHeader = layout.header && content;
  const showFooter = layout.footer && content;

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-slate-50">
      {showHeader && <Header navigation={content?.navigation}></Header>}

      <main className="container m-auto">
        <DotCMSLayoutBody page={pageAsset} components={dotComponents} />
      </main>

      {showFooter && <Footer {...content} />}
    </div>
  );
};
