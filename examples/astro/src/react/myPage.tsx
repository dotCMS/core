import { DotcmsLayout } from "@dotcms/react";

import { usePageAsset } from "./hooks/usePageAsset";
import Header from "./layout/header";

import componentsMap from "./content-types";
import type { FC } from "react";

import { Navigation } from "./layout/navigation";
import { Footer } from "./layout/footer/footer";
import NotFound from "./components/notFound";

import type { DotcmsNavigationItem, DotCMSPageAsset } from "@dotcms/types";

export type MyPageProps = {
  pageAsset?: DotCMSPageAsset;
  nav?: DotcmsNavigationItem[];
};

export const MyPage: FC<MyPageProps> = ({ pageAsset, nav }) => {
  pageAsset = usePageAsset(pageAsset);

  if (!pageAsset) {
    return <NotFound />;
  }

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
      {pageAsset?.layout.header && (
        <Header>
          <Navigation items={nav} />
        </Header>
      )}

      <main className="container m-auto">
        <DotcmsLayout
          pageContext={{
            components: componentsMap,
            pageAsset: pageAsset as any,
            isInsideEditor: false,
          }}
          config={{
            pathname: window.location.pathname,

            editor: {
              params: {
                depth: "3",
              },
            },
          }}
        />
      </main>

      {pageAsset?.layout.footer && <Footer />}
    </div>
  );
};
