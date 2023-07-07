package com.dotcms.api.traversal;

import static com.dotcms.model.asset.BasicMetadataFields.PATH_META_KEY;
import static com.dotcms.model.asset.BasicMetadataFields.SHA256_META_KEY;
import static com.dotcms.model.asset.BasicMetadataFields.SIZE_META_KEY;

import com.dotcms.model.asset.AssetVersionsView;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FilterTest {

    @Test
    public void test_no_filters() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(5, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_includes() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeAsset("*.png")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_includes2() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeAsset("*.txt")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(3, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_includes3() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeAsset("file*")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(3, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_includes4() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir*")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(5, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeAsset("*.txt")
                .includeAsset("*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(4, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes2() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir")
                .includeAsset("*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes3() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir*")
                .includeAsset("*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes4() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir?")
                .includeAsset("*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes5() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir?") // ** does not match root folder
                .includeAsset("**/*.jpeg") // ** does not match root folder
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(0, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes6() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/folderNotExists")
                .includeAsset("**/*.fileNotExists")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(0, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes7() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir?")
                .includeAsset("**/*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/blog/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes8() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir2")
                .includeAsset("**/*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/blog/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(1, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes9() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir2")
                .includeAsset("**/*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(1, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(1, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes10() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir?")
                .includeAsset("*.jpeg")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(0, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_includes11() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir1")
                .includeFolder("**/dir2")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(2, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(5, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_excludes() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeAsset("*.png")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(4, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_excludes2() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeAsset("file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(3, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_excludes3() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeFolder("dir*")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(5, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_simple_excludes4() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeFolder("dir?")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(5, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_excludes() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeFolder("dir?")
                .excludeAsset("file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(3, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_excludes2() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeFolder("dir?")
                .excludeAsset("file*")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(2, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_multiple_excludes3() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeFolder("**/dir1")
                .excludeFolder("**/dir2")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(1, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(5, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_combine() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .excludeFolder("dir?")
                .includeAsset("file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(2, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_combine2() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir?")
                .includeAsset("*.*")
                .excludeAsset("file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(3, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_combine3() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("dir?")
                .includeAsset("*.*")
                .excludeAsset("file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(0, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(0, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_combine4() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir?")
                .includeAsset("*.*")
                .excludeAsset("file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(0, filteredFolderView.assets().versions().size());
    }

    @Test
    public void test_combine5() {

        var filter = Filter.builder()
                .rootPath("/images/")
                .includeFolder("**/dir?")
                .includeAsset("**/*.*")
                .excludeAsset("**/file?.txt")
                .build();

        var folder = folderViewForPath("images", "/images/blog/another/one/more/");

        var filteredFolderView = filter.apply(folder);

        // Test when the path matches an implicitGlobInclude pattern
        Assertions.assertNotNull(filteredFolderView);
        Assertions.assertEquals(3, filteredFolderView.subFolders().stream().
                filter(FolderView::implicitGlobInclude).count());
        Assertions.assertEquals(3, filteredFolderView.assets().versions().size());
    }

    private FolderView folderViewForPath(String name, String path) {

        var folderBuilder = FolderView.builder();
        folderBuilder.name(name).path(path).level(0);

        // Sub-folders
        folderBuilder.addSubFolders(
                simpleFolderViewForPath("dir1", path + "dir1/", 1),
                simpleFolderViewForPath("dir2", path + "dir2/", 1),
                simpleFolderViewForPath("dir3", path + "dir3/", 1)
        );

        // Assets
        var assetVersionViewBuilder = AssetVersionsView.builder();
        assetVersionViewBuilder.addVersions(
                assetViewForPath("file.txt", path),
                assetViewForPath("file1.txt", path),
                assetViewForPath("file2.txt", path),
                assetViewForPath("sky.png", path),
                assetViewForPath("sun.jpeg", path)
        );

        folderBuilder.assets(assetVersionViewBuilder.build());

        return folderBuilder.build();
    }

    private FolderView simpleFolderViewForPath(String name, String path, int level) {
        return FolderView.builder().
                name(name).
                path(path).
                level(level).
                build();
    }

    private AssetView assetViewForPath(String name, String path) {

        var metadata = new HashMap<String, Object>();
        metadata.put(PATH_META_KEY.key(), path);
        metadata.put(SHA256_META_KEY.key(),
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        metadata.put(SIZE_META_KEY.key(), 1000L);

        var asset = AssetView.builder().
                name(name).
                modDate(new Date().toInstant()).
                identifier(UUID.randomUUID().toString()).
                inode(UUID.randomUUID().toString()).
                live(true).
                working(true).
                lang("en-US").
                sortOrder(0).
                build();

        return asset.withMetadata(metadata);
    }

}
