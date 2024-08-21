import { DotcmsLayout } from "@dotcms/react";

import { usePageAsset } from "./hooks/usePageAsset";
import Activity from "./content-types/activity";
import Banner from "./content-types/banner";
import CalendarEvent from "./content-types/calendarEvent";
import CallToAction from "./content-types/callToAction";
import { CustomNoComponent } from "./content-types/empty";
import ImageComponent from "./content-types/image";
import Product from "./content-types/product";
import WebPageContent from "./content-types/webPageContent";
import Header from "./layout/header";
import Navigation from "./layout/navigation";
import Footer from "./layout/footer/footer";

// Mapping of components to DotCMS content types
const componentsMap = {
  webPageContent: WebPageContent,
  Banner: Banner,
  Activity: Activity,
  Product: Product,
  Image: ImageComponent,
  calendarEvent: CalendarEvent,
  CallToAction: CallToAction,
  CustomNoComponent: CustomNoComponent,
};

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
