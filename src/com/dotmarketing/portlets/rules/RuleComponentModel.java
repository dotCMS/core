package com.dotmarketing.portlets.rules;

import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.Map;

/**
 * @author Geoff M. Granum
 */
public interface RuleComponentModel {

    Map<String,ParameterModel> getParameters();
}
