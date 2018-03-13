package com.test.Statistics.service;

import com.test.Statistics.domain.Data;
import com.test.Statistics.domain.Statistic;

public interface Statistics
{
    Statistic getStatistic();

    void addData(Data data);
}
