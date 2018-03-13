package com.test.Statistics.domain;

import com.test.Statistics.domain.Data;

import java.util.Comparator;

public class DataMinAmountComparator implements Comparator<Data>
{

    @Override
    public int compare(Data o1, Data o2)
    {
        return o1.getAmount().compareTo(o2.getAmount());
    }
}