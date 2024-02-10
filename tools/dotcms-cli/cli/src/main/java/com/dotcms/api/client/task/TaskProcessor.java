package com.dotcms.api.client.task;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.traversal.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

public class TaskProcessor {

    private static final int MAX_RETRIES = 3;// Maximum number of retries for a single task
    private static final int TASK_TIMEOUT = 15;// In seconds
    protected static final int THRESHOLD = 10;

    /**
     * Processes and waits for the results of the tasks submitted to the completion service.
     *
     * @param toProcessCount    The number of tasks to process.
     * @param completionService The CompletionService for parallel execution of tasks.
     */
    protected <T> List<T> processTasks(int toProcessCount,
            CompletionService<List<T>> completionService) {

        List<T> results = new ArrayList<>();

        boolean interrupted = false;
        int retryCount = 0;
        int taskCount = 0;

        // Wait for all tasks to complete and gather the results
        while (taskCount < toProcessCount) {

            try {

                Future<List<T>> future = completionService.poll(
                        TASK_TIMEOUT, TimeUnit.SECONDS
                );
                if (future != null) {
                    // Task completed, process the result
                    List<T> taskResult = future.get();
                    results.addAll(taskResult);

                    taskCount++;
                    retryCount = 0; // Reset retry count after a successful operation
                } else {
                    // No task was completed in the given timeframe
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                    } else {
                        throw new TraversalTaskException(
                                "Maximum retries reached for fetching task result"
                        );
                    }
                }
            } catch (InterruptedException e) {
                // Thread was interrupted, handle it outside the loop
                interrupted = true;
                Thread.currentThread().interrupt(); // Preserve interrupt status
            } catch (ExecutionException e) {
                handleExecutionException(e);
            }
        }

        if (interrupted) {
            handleInterrupt();
        }

        return results;
    }

    /**
     * Processes and waits for the results of the tasks submitted to the completion service.
     *
     * @param toProcessCount    The number of tasks to process.
     * @param completionService The CompletionService for parallel execution of traversal tasks.
     * @param errors            The list of exceptions to which any error should be added.
     * @param currentNode       The current TreeNode representing the current folder.
     */
    protected void processTasks(int toProcessCount,
            CompletionService<Pair<List<Exception>, TreeNode>> completionService,
            ArrayList<Exception> errors, TreeNode currentNode) {

        boolean interrupted = false;
        int retryCount = 0;
        int taskCount = 0;

        // Wait for all tasks to complete and gather the results
        while (taskCount < toProcessCount) {

            try {

                Future<Pair<List<Exception>, TreeNode>> future = completionService.poll(
                        TASK_TIMEOUT, TimeUnit.SECONDS
                );
                if (future != null) {
                    // Task completed, process the result
                    Pair<List<Exception>, TreeNode> taskResult = future.get();
                    errors.addAll(taskResult.getLeft());
                    currentNode.addChild(taskResult.getRight());

                    taskCount++;
                    retryCount = 0; // Reset retry count after a successful operation
                } else {
                    // No task was completed in the given timeframe
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                    } else {
                        throw new TraversalTaskException(
                                "Maximum retries reached for fetching task result"
                        );
                    }
                }
            } catch (InterruptedException e) {
                // Thread was interrupted, handle it outside the loop
                interrupted = true;
                Thread.currentThread().interrupt(); // Preserve interrupt status
            } catch (ExecutionException e) {
                handleExecutionException(e);
            }
        }

        if (interrupted) {
            handleInterrupt();
        }
    }

    /**
     * Handles the case when a task execution throws an ExecutionException.
     *
     * @param e The ExecutionException encountered.
     * @throws TraversalTaskException with the cause of the execution failure.
     */
    private void handleExecutionException(ExecutionException e) throws TraversalTaskException {
        if (e.getCause() instanceof TraversalTaskException) {
            throw (TraversalTaskException) e.getCause();
        } else {
            throw new TraversalTaskException("Error executing task", e);
        }
    }

    /**
     * Handles the situation when the task processing is interrupted.
     *
     * @throws TraversalTaskException indicating that the task was interrupted.
     */
    private void handleInterrupt() throws TraversalTaskException {
        throw new TraversalTaskException("Task was interrupted and may not have completed fully");
    }

}
