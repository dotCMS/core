import { DotcmsLayout } from "@dotcms/react";

import { usePageAsset } from "./hooks/usePageAsset";
import Header from "./layout/header";

import componentsMap from "./content-types";
import type { FC } from "react";
import type { DotcmsNavigationItem, DotCMSPageAsset } from "../types";
import { Navigation } from "./layout/navigation";
import { Footer } from "./layout/footer/footer";

export type MyPageProps = {
  pageAsset: DotCMSPageAsset | undefined;
  nav: DotcmsNavigationItem[] | undefined;
};

export const MyPage: FC<MyPageProps> = ({ pageAsset, nav }) => {
  pageAsset = usePageAsset(pageAsset);

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
      {pageAsset?.layout.header && (
        <Header>
          <Navigation items={nav} />
        </Header>
      )}

      <main className="container flex flex-col gap-8 m-auto">
        <DotcmsLayout
          pageContext={{
            components: componentsMap,
            pageAsset: pageAsset as any,
            isInsideEditor: false,
          }}
          config={{
            pathname: "/",
          }}
        />
      </main>

      {pageAsset?.layout.footer && <Footer />}
    </div>
  );
};
