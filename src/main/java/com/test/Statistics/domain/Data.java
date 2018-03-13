package com.test.Statistics.domain;

import java.math.BigDecimal;

public class Data implements Comparable<Data>
{
    private BigDecimal amount;
    private long time;

    public Data(BigDecimal  amount, long time)
    {
        this.amount = amount;
        this.time = time;
    }

    public BigDecimal  getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal  amount)
    {
        this.amount = amount;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    @Override
    public int compareTo(Data o)
    {
        return (int)(this.time - o.time);
    }

    @Override
    public String toString()
    {
        return "Data{" +
                "amount=" + amount +
                ", time=" + time +
                '}';
    }
}