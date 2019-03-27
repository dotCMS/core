package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.dotcms.util.CollectionsUtils.*;
import static org.junit.Assert.*;

/**
 * CounterSet unit test.
 * @author jsanca
 */
public class CounterSetTest extends UnitTestBase {


    /**
     * Testing the new Instance
     *
     */
    @Test
    public void getCommonItems_3_repeat_20_items()  {

        final CounterSet<WorkflowAction> counterSet =
                new CounterSet<>(3);

        final WorkflowAction[] workflowActions = new WorkflowAction[] {
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction()
        };

        int index = 0;

        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("98765433");
        workflowActions[index++].setId("56478912");
        workflowActions[index++].setId("56478912");
        workflowActions[index++].setId("98765433");
        workflowActions[index++].setId("98765433");
        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("88877777");
        workflowActions[index++].setId("88877777");
        workflowActions[index++].setId("99999999");
        workflowActions[index++].setId("00000000");
        workflowActions[index++].setId("1111111");
        workflowActions[index++].setId("2222222");
        workflowActions[index++].setId("3333333");
        workflowActions[index++].setId("12245679");
        workflowActions[index++].setId("44444444");
        workflowActions[index++].setId("12245679");
        workflowActions[index++].setId("12245679");

        for (final WorkflowAction action : workflowActions) {

            counterSet.add(action);
        }

        final Collection<WorkflowAction> repeatedItems = counterSet.getCommonItems();
        final Collection<WorkflowAction> allItems      = counterSet.getAllItems();

        assertNotNull(repeatedItems);
        assertNotNull(allItems);

        assertEquals(3, repeatedItems.size());
        assertEquals(11, allItems.size());

        assertTrue  (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("12345678")));
        assertTrue  (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("98765433")));
        assertTrue  (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("12245679")));
        assertFalse (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("3333333")));
        assertFalse (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("56478912")));
    }


    @Test
    public void getCommonItems_3_repeat_20_items_concurrent() throws ExecutionException, InterruptedException {

        final CounterSet<WorkflowAction> counterSet =
                new CounterSet<>(3);

        final WorkflowAction[] workflowActions = new WorkflowAction[] {
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(),
                new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction(), new WorkflowAction()
        };

        int index = 0;

        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("98765433");
        workflowActions[index++].setId("56478912");
        workflowActions[index++].setId("56478912");
        workflowActions[index++].setId("98765433");
        workflowActions[index++].setId("98765433");
        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("12345678");
        workflowActions[index++].setId("88877777");
        workflowActions[index++].setId("88877777");
        workflowActions[index++].setId("99999999");
        workflowActions[index++].setId("00000000");
        workflowActions[index++].setId("1111111");
        workflowActions[index++].setId("2222222");
        workflowActions[index++].setId("3333333");
        workflowActions[index++].setId("12245679");
        workflowActions[index++].setId("44444444");
        workflowActions[index++].setId("12245679");
        workflowActions[index++].setId("12245679");

        workflowActions[index++].setId("0000999");
        workflowActions[index++].setId("0009999");
        workflowActions[index++].setId("0099999");
        workflowActions[index++].setId("0999999");
        workflowActions[index++].setId("999999");

        workflowActions[index++].setId("00001");
        workflowActions[index++].setId("00011");
        workflowActions[index++].setId("00111");
        workflowActions[index++].setId("01111");
        workflowActions[index++].setId("11111");

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter();
        final List<Future> futures = new ArrayList<>();
        for (final WorkflowAction action : workflowActions) {

            futures.add(dotSubmitter.submit(()-> counterSet.add(action)));
        }

        for (final Future future : futures) {

            future.get();
        }

        final Collection<WorkflowAction> repeatedItems = counterSet.getCommonItems();
        final Collection<WorkflowAction> allItems      = counterSet.getAllItems();

        assertNotNull(repeatedItems);
        assertNotNull(allItems);

        assertEquals(3, repeatedItems.size());
        assertEquals(21, allItems.size());

        assertTrue  (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("12345678")));
        assertTrue  (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("98765433")));
        assertTrue  (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("12245679")));
        assertFalse (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("3333333")));
        assertFalse (repeatedItems.stream().anyMatch(workflowAction -> workflowAction.getId().equals("56478912")));
    }


}