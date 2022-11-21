package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import io.vavr.control.Try;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * This Actionlet will allow a user to stop an HTTP Request and return a specific HTTP Status Code.
 *
 * @author Will Ezell
 * @since Oct 24th, 2022
 */
public class StopProcessingActionlet extends RuleActionlet<StopProcessingActionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String RETURN_CODE = "return-code";

    public StopProcessingActionlet() {
        super("api.system.ruleengine.actionlet.StopProcessingActionlet", new ParameterDefinition<>(1, RETURN_CODE,
                new DropdownInput().minSelections(1).maxSelections(1)
                        .option("200", "200")
                        .option("202", "202")
                        .option("204", "204")
                        .option("301", "301")
                        .option("302", "302")
                        .option("401", "401")
                        .option("403", "403")
                        .option("405", "405")
                        .option("500", "500")
                        .option("501", "501")
                        .allowAdditions()));
    }

    @Override
    public Instance instanceFrom(final Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(final HttpServletRequest request, final HttpServletResponse response,
                            final Instance instance) {
        if (response.isCommitted()) {
            return false;
        }
        response.setStatus(instance.responseCode);
        response.setContentLength(0);
        Try.run(() -> {
            response.getWriter().flush();
            response.getWriter().close();
        });
        return true;
    }

    public class Instance implements RuleComponentInstance {

        private final int responseCode;

        public Instance(final Map<String, ParameterModel> parameters) {
            assert parameters != null;
            this.responseCode =
                    Try.of(() -> Integer.parseInt(parameters.get(RETURN_CODE).getValue())).getOrElse(HttpStatus.SC_OK);
        }

    }

}
