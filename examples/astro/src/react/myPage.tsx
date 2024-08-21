import { DotcmsLayout } from "@dotcms/react";

import { usePageAsset } from "./hooks/usePageAsset";
import Header from "./layout/header";
import Navigation from "./layout/navigation";
import Footer from "./layout/footer/footer";
import componentsMap from "./content-types";



export function MyPage({ pageAsset, nav }: any) {
  pageAsset = usePageAsset(pageAsset);

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
      {pageAsset.layout.header && (
                <Header>
                    <Navigation items={nav} />
                 </Header>
             )}

      <main className="container flex flex-col gap-8 m-auto">
        <DotcmsLayout
          pageContext={{
            components: componentsMap,
            pageAsset: pageAsset,
          }}
          config={{
            pathname: "/",
          }}
        />
      </main>

      {pageAsset.layout.footer && <Footer />}
    </div>
  );
}
