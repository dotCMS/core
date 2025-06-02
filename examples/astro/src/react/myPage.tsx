import type { DotCMSNavigationItem, DotCMSPageAsset } from "@dotcms/types";
import { DotCMSLayoutBody } from "@dotcms/react/next";

import { pageComponents } from "./content-types";

import { Footer } from "./layout/footer/footer";
import { Navigation } from "./layout/navigation";
import Header from "./layout/header";
import NotFound from "./components/notFound";

export type MyPageProps = {
  pageAsset?: DotCMSPageAsset;
  nav?: DotCMSNavigationItem[];
};

export const MyPage = ({ pageAsset, nav }: MyPageProps) => {
  // pageAsset = usePageAsset(pageAsset);

  console.log(pageAsset);

  if (!pageAsset) {
    return <NotFound />;
  }

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
      {pageAsset?.layout.header && (
        <Header>
          <h1>Header</h1>
          {/* <Navigation items={nav} /> */}
        </Header>
      )}

      <main className="container m-auto">
        <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
      </main>

      {pageAsset?.layout.footer && <Footer />}
    </div>
  );
};
