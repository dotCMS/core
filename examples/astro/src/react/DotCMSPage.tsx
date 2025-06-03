import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react/next";

import { pageComponents } from "../components/content-types";

import Footer from "@components/react/Footer";
import Header from "@components/react/Header";

export type MyPageProps = {
  pageResponse: any;
};

export const DotCMSPage = ({ pageResponse }: any) => {
  const { pageAsset, content = {} } = useEditableDotCMSPage<any>(pageResponse);
  const navigation = content.navigation;

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-slate-50">
      {pageAsset?.layout.header && <Header navItems={navigation.children}></Header>}

      <main className="container m-auto">
        <DotCMSLayoutBody page={pageAsset} components={pageComponents} />
      </main>

      {pageAsset?.layout.footer && (
        <Footer {...content} />
      )}
    </div>
  );
};
