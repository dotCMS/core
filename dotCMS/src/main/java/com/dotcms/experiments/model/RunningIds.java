package com.dotcms.experiments.model;

import com.dotcms.experiments.model.RunningIds.RunningId;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RunningIds extends AbstractList<RunningId> {
    private List<RunningId> runningIds = new ArrayList<>();

    RunningIds(){}

    @Override
    public RunningId get(int index) {
        return null;
    }

    @Override
    public int size() {
        return runningIds.size();
    }

    @Override
    public boolean add(RunningId runningId) {
        runningIds.add(runningId);
        return true;
    }

    public static class RunningId {}
}
