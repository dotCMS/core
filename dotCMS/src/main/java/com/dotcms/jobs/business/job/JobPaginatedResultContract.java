package com.dotcms.jobs.business.job;

import java.util.List;

public interface JobPaginatedResultContract <T extends JobContract> {

    List<T> jobs();

    long total();

    int page();

    int pageSize();

}
