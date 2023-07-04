package com.dotcms.experiments.model;

import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class RunningIds {
    @JsonProperty()
    private List<RunningId> ids = new ArrayList<>();

    public RunningIds(){}

    public RunningId get(int index) {
        return ids.get(index);
    }

    public Iterator<RunningId> iterator() {
        return ids.iterator();
    }

    public int size() {
        return ids.size();
    }

    public boolean add(RunningId runningId) {
        ids.add(runningId);
        return true;
    }

    @JsonIgnore
    public Collection<RunningId> getAll() {
        return ImmutableList.copyOf(ids);
    }

    public static class RunningId {

        @JsonProperty()
        private String id;
        @JsonProperty()
        private Instant startDate;
        @JsonProperty()
        private Instant endDate;

        @JsonCreator
        RunningId(final @JsonProperty("id") String id, final  @JsonProperty("startDate") Instant startDate,
                final  @JsonProperty("endDate") Instant endDate) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public static RunningId create() {
            return new RunningId(UUIDGenerator.generateUuid(), Instant.now(), null);
        }
        public String id() {
            return id;
        }

        public Instant startDate() {
            return startDate;
        }

        public Instant endDate() {
            return endDate;
        }
        public void setEndDate(Instant endDate) {
            this.endDate = endDate;
        }

    }
}
