package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

public class WorkflowSchemeAssertionChecker implements AssertionChecker<WorkflowScheme> {
    @Override
    public Map<String, Object> getFileArguments(final WorkflowScheme workflowScheme, File file) {
        return map(
                "name", workflowScheme.getName(),
                "description", workflowScheme.getDescription(),
                "id", workflowScheme.getId()
        );
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/workflow/workflow.workflow.xml";
    }

    @Override
    public File getFileInner(final WorkflowScheme workflowScheme, File bundleRoot) {
        return FileBundlerTestUtil.getWorkflowFilePath(workflowScheme, bundleRoot);
    }

    @Override
    public Collection<String> getRegExToRemove(File file) {
        return list(
                "<modDate class=\"sql-timestamp\">.*</modDate>",
                "<creationDate>.*</creationDate>"
        );
    }

    @Override
    public boolean checkFileContent(final WorkflowScheme workflowScheme) {
        return !workflowScheme.isSystem();
    }
}
