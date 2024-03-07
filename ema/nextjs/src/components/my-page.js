"use client";

import { DotcmsLayout } from "@dotcms/react";

import WebPageContent from "./content-types/webPageContent";
import Banner from "./content-types/banner";
import Activity from "./content-types/activity";
import Product from "./content-types/product";
import ImageComponent from "./content-types/image";

import Header from "./layout/header";
import Footer from "./layout/footer";
import Navigation from "./layout/navigation";
import { usePathname, useRouter } from "next/navigation";

export function MyPage({ data, nav }) {
  const { refresh } = useRouter();
  const pathname = usePathname();

  return (
    <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
      {data.layout.header && (
        <Header>
          <Navigation items={nav} />
        </Header>
      )}
      <main className="container flex flex-col gap-8 m-auto">
        <DotcmsLayout
          entity={{
            // These are the components that will be used to render the contentlets in the page.
            components: {
              webPageContent: WebPageContent,
              Banner: Banner,
              Activity: Activity,
              Product: Product,
              Image: ImageComponent,
            },
            ...data,
          }}
          options={{ onReload: refresh, pathname }}
        />
      </main>
      {data.layout.footer && <Footer />}
    </div>
  );
}
