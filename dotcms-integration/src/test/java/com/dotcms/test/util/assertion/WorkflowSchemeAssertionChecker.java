package com.dotcms.test.util.assertion;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link AssertionChecker} concrete class for {@link WorkflowScheme}
 */
public class WorkflowSchemeAssertionChecker implements AssertionChecker<WorkflowScheme> {
    @Override
    public Map<String, Object> getFileArguments(final WorkflowScheme workflowScheme, File file) {
        try {
            final List<WorkflowStep> steps = APILocator.getWorkflowAPI().findSteps(workflowScheme);

            final Map<String, Object> map = new HashMap<>(Map.of(
                    "name", workflowScheme.getName(),
                    "description", workflowScheme.getDescription(),
                    "id", workflowScheme.getId()
            ));

            if (!steps.isEmpty()) {
                final WorkflowStep workflowStep = steps.get(0);

                map.put("step_id", workflowStep.getId());
                map.put("step_name", workflowStep.getName());

                final List<WorkflowAction> actions = APILocator.getWorkflowAPI()
                        .findActions(workflowStep, APILocator.systemUser());

                if (!actions.isEmpty()) {
                    map.put("action_id", actions.get(0).getId());
                    map.put("action_name", actions.get(0).getName());
                }
            }

            return map;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
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
                "<creationDate>.*</creationDate>",
                "<modDate class=\"sql-timestamp\">.*</modDate>",
                "<modDate>.*</modDate>",
                "<nextAssign>.*</nextAssign>",
                "<com.dotmarketing.portlets.workflows.model.WorkflowState>.*</com.dotmarketing.portlets.workflows.model.WorkflowState>",
                "<condition></condition>"
        );
    }

    @Override
    public boolean checkFileContent(final WorkflowScheme workflowScheme) {
        return !workflowScheme.isSystem();
    }
}
