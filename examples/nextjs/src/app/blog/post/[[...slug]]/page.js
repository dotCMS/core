import NotFound from "@/app/not-found";
import { DetailPage } from "@/views/DetailPage";
import { getDotCMSPage } from "@/utils/getDotCMSPage";

export async function generateMetadata(props) {
    const params = await props.params;
    try {
        const path = params.slug[0];
        const { pageAsset } = await getDotCMSPage(`/blog/post/${path}`);
        const urlContentMap = pageAsset?.urlContentMap;
        const title = urlContentMap?.title || 'Page not found';
        return {
            title: `${title} - Blog`
        };
    } catch (e) {
        return {
            title: 'not found'
        };
    }
}

export default async function Home(props) {
    const params = await props.params;
    const path = params.slug[0];
    const pageContent = await getDotCMSPage(`/blog/post/${path}`);

    const vanityUrl = pageContent?.pageAsset?.vanityUrl;
    const action = vanityUrl?.action ?? 0;

    if (action > 200) {
        return redirect(pageContent.pageAsset.vanityUrl.forwardTo);
    }

    if (!pageContent) {
        return <NotFound />;
    }

    return <DetailPage pageContent={pageContent} />;
}
