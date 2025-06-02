import type { DotCMSNavigationItem, DotCMSPageAsset } from "@dotcms/types";
import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react/next";

import { pageComponents } from "../components/content-types";

import Footer from "./layout/footer/Footer";
import Header from "./layout/Header";
import NotFound from "../components/react/notFound";

export type MyPageProps = {
  pageResponse: any;
};

export const DotCMSPage = ({ pageResponse }: any) => {
  const { pageAsset, content = {} } = pageResponse; //useEditableDotCMSPage<any>(pageResponse);
  const navigation = content.navigation;

  console.log(content);

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
      {pageAsset?.layout.header && <Header navItems={navigation.children}></Header>}

      <main className="container m-auto">
        <DotCMSLayoutBody page={pageAsset} components={pageComponents} mode="development" />
      </main>

      {pageAsset?.layout.footer && (
        <Footer {...content} />
      )}
    </div>
  );
};
