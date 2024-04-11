package com.dotcms.util.diff;

import com.dotcms.UnitTestBase;
import com.dotcms.util.CollectionsUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * DiffCommandTest unit test.
 * @author jsanca
 */
public class DiffCommandTest extends UnitTestBase {

    @Test
    public void applyDiff_Differentiable() {

        final Map<String, StringDifferentiable> currentObjects = Map.of(
                "one", new StringDifferentiable("one"), "two", new StringDifferentiable("two"),
                "three", new StringDifferentiable("three"), "four", new StringDifferentiable("four"));
        final Map<String, StringDifferentiable> newObjects     =  Map.of(
                "one", new StringDifferentiable("onexxx"),
                "three", new StringDifferentiable("three"), "four", new StringDifferentiable("fourxxx"),
                "five", new StringDifferentiable("five"), "six", new StringDifferentiable("six"));

        final DiffCommand<String, StringDifferentiable, String, StringDifferentiable> diffCommand = new SimpleDiffCommand<>();
        final DiffResult<String, StringDifferentiable> diffResult = diffCommand.applyDiff(currentObjects, newObjects);

        Assert.assertNotNull(diffResult);
        Assert.assertNotNull(diffResult.getToAdd());
        Assert.assertNotNull(diffResult.getToDelete());
        Assert.assertNotNull(diffResult.getToUpdate());
        Assert.assertFalse(diffResult.getToAdd().isEmpty());
        Assert.assertFalse(diffResult.getToDelete().isEmpty());
        Assert.assertFalse(diffResult.getToUpdate().isEmpty());

        System.out.println(diffResult.getToAdd());
        Assert.assertTrue(diffResult.getToAdd().containsKey("five"));
        Assert.assertTrue(diffResult.getToAdd().containsKey("six"));

        System.out.println(diffResult.getToDelete());
        Assert.assertTrue(diffResult.getToDelete().containsKey("two"));

        System.out.println(diffResult.getToUpdate());
        Assert.assertTrue(diffResult.getToUpdate().containsKey("one"));
        Assert.assertTrue(diffResult.getToUpdate().containsKey("four"));
    }

    @Test
    public void applyDiff_Comparable() {

        final Map<String, String> currentObjects = Map.of(
                "one", "one", "two", "two",
                "three", "three", "four", "four");
        final Map<String, String> newObjects     =  Map.of(
                "one", "onexxx",
                "three", "three", "four", "fourxxx",
                "five", "five", "six", "six");

        final DiffCommand<String, String, String, String> diffCommand = new SimpleDiffCommand<>();
        final DiffResult<String, String> diffResult = diffCommand.applyDiff(currentObjects, newObjects);

        Assert.assertNotNull(diffResult);
        Assert.assertNotNull(diffResult.getToAdd());
        Assert.assertNotNull(diffResult.getToDelete());
        Assert.assertNotNull(diffResult.getToUpdate());
        Assert.assertFalse(diffResult.getToAdd().isEmpty());
        Assert.assertFalse(diffResult.getToDelete().isEmpty());
        Assert.assertFalse(diffResult.getToUpdate().isEmpty());

        System.out.println(diffResult.getToAdd());
        Assert.assertTrue(diffResult.getToAdd().containsKey("five"));
        Assert.assertTrue(diffResult.getToAdd().containsKey("six"));

        System.out.println(diffResult.getToDelete());
        Assert.assertTrue(diffResult.getToDelete().containsKey("two"));

        System.out.println(diffResult.getToUpdate());
        Assert.assertTrue(diffResult.getToUpdate().containsKey("one"));
        Assert.assertTrue(diffResult.getToUpdate().containsKey("four"));
    }

    private class StringDifferentiable implements Differentiable<StringDifferentiable> {

        final String value;

        public StringDifferentiable(final String value) {
            this.value = value;
        }

        @Override
        public boolean isDiff(final StringDifferentiable s) {

            return !this.value.equals(s.value);
        }
    }


    private class SimpleDiffCommand<T> implements DiffCommand<String, T, String, T> {

        @Override
        public DiffResult<String, T> applyDiff(final Map<String, T> currentObjects,
                                                    final Map<String, T> newObjects) {

            final DiffResult.Builder<String, T> builder = new DiffResult.Builder<>();

            final Map<String, T> fieldsToDelete = currentObjects.entrySet().stream()
                    .filter(entry ->  !newObjects.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

            final Map<String, T> fieldsToAdd    = newObjects.entrySet().stream()
                    .filter(entry ->  !currentObjects.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

            final Map<String, T> fieldsToUpdate = newObjects.entrySet().stream()
                    .filter(entry -> currentObjects.containsKey(entry.getKey())
                            &&  this.isDiff(entry.getValue(), currentObjects.get(entry.getKey())))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

            return builder.putAllToDelete(fieldsToDelete).
                    putAllToAdd(fieldsToAdd).putAllToUpdate(fieldsToUpdate).build();
        }

        private <T> boolean isDiff(final T newValue, final T currentValue) {

            if (newValue instanceof Differentiable) {

                return Differentiable.class.cast(newValue).isDiff(currentValue);
            }

            if (newValue instanceof Comparable) {

                return Comparable.class.cast(newValue).compareTo(currentValue) != 0;
            }

            return !newValue.equals(currentValue);
        }
    }
}
