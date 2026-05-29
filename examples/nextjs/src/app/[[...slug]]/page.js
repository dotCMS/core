import { redirect } from "next/navigation";
import NotFound from "@/app/not-found";
import { ErrorPage, ERROR_COPY } from "@/components/error";
import { getDotCMSPage } from "@/utils/getDotCMSPage";
import { Page } from "@/views/Page";


export async function generateMetadata(props) {
    const params = await props.params;
    const path = params?.slug?.join("/") || "/";
    const pageContent = await getDotCMSPage(path);
    if (pageContent?.error) {
        const copy = ERROR_COPY[pageContent.error.status] ?? ERROR_COPY.default;
        return { title: copy.heading };
    }

    const title = pageContent?.pageAsset?.page?.friendlyName || pageContent?.pageAsset?.page?.title;
    return { title: title || "Not Found" };
}

export default async function Home(props) {
    const params = await props.params;
    const path = params?.slug?.join("/") || "/";
    const pageContent = await getDotCMSPage(path);

    if (pageContent?.error) {
        return <ErrorPage error={pageContent.error} />;
    }

    const vanityUrl = pageContent?.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200) {
        return redirect(pageContent.pageAsset.vanityUrl.forwardTo);
    }

    if (!pageContent) {
        return <NotFound />;
    }

    return <Page pageContent={pageContent} />;
}
