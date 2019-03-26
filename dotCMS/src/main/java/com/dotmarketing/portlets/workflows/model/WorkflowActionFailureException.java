package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.exception.DotRuntimeException;
import java.io.Serializable;

public class WorkflowActionFailureException extends DotRuntimeException implements Serializable {

  public WorkflowActionFailureException(String x, Exception e) {
    super(x, e);
  }

  public WorkflowActionFailureException(String x) {
    super(x);
  }
}
