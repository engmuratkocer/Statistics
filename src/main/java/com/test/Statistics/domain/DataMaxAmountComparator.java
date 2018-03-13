package com.test.Statistics.domain;

import com.test.Statistics.domain.Data;

import java.util.Comparator;

public class DataMaxAmountComparator implements Comparator<Data>
{

    @Override
    public int compare(Data o1, Data o2)
    {
        return o2.getAmount().compareTo(o1.getAmount());
    }
}