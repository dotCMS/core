package com.dotcms.rendering.velocity.viewtools;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Shared scenario for customer regression dotCMS #37870 (follow-up to #36026): through
 * {@code $estool.search()}, the customer's verbatim {@code $!{record.contentTypeId}} lookup must
 * resolve to their <b>custom</b> field value ({@code "10"}), never the 32-char content type inode.
 *
 * <p>The customer's field velocity var name is {@code contentTypeId}, which collides with the
 * built-in {@code Contentlet#getContentTypeId()} getter ({@code map.get("stInode")}). On the
 * raw-{@link Contentlet} path (deprecated {@code $estool.esSearch()}) that getter shadows the custom
 * field; on {@code $estool.search()} the records are {@code ContentMap}, which has no such getter and
 * resolves {@code .contentTypeId} through {@code ContentMap.get("contentTypeId")} = the field value.</p>
 *
 * <h3>One scenario, parameterized by phase</h3>
 * <p>This base holds the entire scenario (content type + contentlet + VTL + assertions); the
 * migration phase is the single hook, supplied by {@link #migrationPhase()}. The only concrete today
 * is {@code ContentTypeIdSearchResolutionESTest} (MainSuite1b), which runs it under phase 1
 * (dual-write, <b>ES reads</b>). The phase is abstracted so the same scenario can be re-run on a
 * read-from-OS phase once a suite supports a full content lifecycle against OpenSearch.</p>
 *
 * <p><b>Why a read-from-OS concrete is not added here:</b> the assertion outcome is phase-independent
 * because {@code search()} is DB-backed — both {@code ESSearchAPIImpl}/{@code OSSearchAPIImpl}
 * discover hit inodes from the index, then load the full {@link Contentlet}s via
 * {@code findContentlets(inodes)} — so {@code ContentMap}'s field resolution is identical regardless
 * of which store served the hit. The OpenSearch read path itself (the query finds the doc and returns
 * the correct inode) is covered by {@code OSSearchAPIImplIntegrationTest}. A high-level
 * publish → search round trip cannot run in the {@code OpenSearchUpgradeSuite} today because that
 * environment has no OS content-index bootstrap (write/read index resolution diverges — see #36054).</p>
 */
public abstract class ContentTypeIdSearchResolutionTestBase extends IntegrationTestBase {

    protected User systemUser;
    protected Host defaultHost;
    protected Language defaultLanguage;

    /**
     * The migration phase ordinal under which the scenario runs (0=ES only, 1=dual-write/ES reads,
     * 2=dual-write/OS reads, 3=OS only). The ES concrete uses a read-from-ES phase.
     */
    protected abstract int migrationPhase();

    @Before
    public void init() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
        defaultHost = APILocator.getHostAPI().findDefaultHost(systemUser, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
    }

    @Test
    public void searchResolvesCustomContentTypeIdField_notTheTypeInode() throws Exception {
        // A content type whose custom field collides by name with Contentlet#getContentTypeId().
        final Field customContentTypeId = new FieldDataGen()
                .name("contentTypeId").velocityVarName("contentTypeId")
                .type(TextField.class).indexed(true).next();
        final ContentType contentType = new ContentTypeDataGen()
                .name("ContentTypeIdCollision QA " + System.currentTimeMillis())
                .host(defaultHost)
                .field(customContentTypeId)
                .nextPersisted();

        final String previousPhase = Config.getStringProperty(FLAG_KEY, null);
        Config.setProperty(FLAG_KEY, String.valueOf(migrationPhase()));
        try {
            final Contentlet contentlet = new ContentletDataGen(contentType)
                    .host(defaultHost)
                    .languageId(defaultLanguage.getId())
                    .setProperty("contentTypeId", "10")
                    .nextPersisted();
            ContentletDataGen.publish(contentlet);
            APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode(), true);

            // The customer reads $!{record.contentTypeId} verbatim. Query is lowercased by search(),
            // so the camelCase contentType var folds to the physical `contenttype` index field.
            final String customerVtl = """
                    #set($esQuery = '{
                        "query": { "query_string": { "query": "+contentType:%s +live:true" } },
                        "size": 5
                    }')
                    #set($results = $estool.search($esQuery))
                    totalResults: $!{results.totalResults}
                    #foreach($record in $results)
                      id: $!{record.identifier} ctid: $!{record.contentTypeId}
                    #end
                    """.formatted(contentType.variable());

            final String output = VelocityUtil.eval(customerVtl, velocityContext());
            Logger.info(this, "\n===== #37870 [phase " + migrationPhase() + "] search() VTL output =====\n"
                    + output + "\n================================");

            assertTrue("The search() result loop must run (the customer's content must be found)",
                    Pattern.compile("ctid:\\s*\\S").matcher(output).find());
            assertTrue("FIX/RECOMMENDATION (#37870): through search()/ContentMap, $record.contentTypeId "
                            + "must resolve to the custom field value '10'",
                    Pattern.compile("ctid:\\s*10\\b").matcher(output).find());
            assertFalse("$record.contentTypeId must NOT return a 32-char content-type inode hash "
                            + "(the raw-Contentlet getter-shadowing bug)",
                    Pattern.compile("ctid:\\s*[0-9a-fA-F]{32}\\b").matcher(output).find());
            assertFalse("$record.contentTypeId must NOT return the actual content type inode",
                    output.contains("ctid: " + contentType.id()));
        } finally {
            Config.setProperty(FLAG_KEY, previousPhase);
        }
    }

    /** Initializes an {@link ESContentTool} in LIVE mode with the system user (à la ContentToolTest). */
    protected ESContentTool liveContentTool() {
        final ViewContext viewContext = mock(ViewContext.class);
        final Context velocityContext = mock(Context.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getParameter("host_id")).thenReturn(defaultHost.getInode());
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE.name());
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final ESContentTool tool = new ESContentTool();
        tool.init(viewContext);
        return tool;
    }

    /** Builds a Velocity context with the live {@code $estool} and a mock {@code $response} bound. */
    protected Context velocityContext() {
        final Context ctx = new VelocityContext();
        ctx.put("estool", liveContentTool());
        ctx.put("response", mock(HttpServletResponse.class));
        return ctx;
    }
}
