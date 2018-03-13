package com.test.Statistics.domain;

import com.test.Statistics.domain.Data;

import java.util.Comparator;

public class DataTimeComparator implements Comparator<Data>
{

    @Override
    public int compare(Data o1, Data o2)
    {
        return (int)(o1.getTime() - o2.getTime());
    }
}