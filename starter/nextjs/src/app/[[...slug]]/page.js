import { MyPage } from "@/components/my-page";
import { client } from "@/utils/client";

export default async function Home({ params }) {
    const slug = params.slug?.join('/') || '/'

    const pageAsset = await client.page.get({
        path: slug || '',
        depth: 1,
    });

    return <MyPage pageAsset={pageAsset} />;
}
