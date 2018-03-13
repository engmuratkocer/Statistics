package com.test.Statistics.service;

import com.test.Statistics.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class StatisticsImpl implements Statistics
{
    private static final Logger LOGGER = Logger.getLogger(StatisticsImpl.class.getName());

    protected static int EXPIRY_SECONDS = 60;
    protected static final int QUEUE_SIZE = 100000;

    protected static final PriorityBlockingQueue<Data> dataQueue = new PriorityBlockingQueue<Data>(QUEUE_SIZE, new DataTimeComparator());
    protected static final PriorityBlockingQueue<Data> minDataQueue = new PriorityBlockingQueue<Data>(QUEUE_SIZE, new DataMinAmountComparator());
    protected static final PriorityBlockingQueue<Data> maxDataQueue = new PriorityBlockingQueue<Data>(QUEUE_SIZE, new DataMaxAmountComparator());

    private static final int RUNNING_PERIOD = 1;
    private static final int DELAY = 1;
    private static BigDecimal totalSum = BigDecimal.ZERO;
    private static BigDecimal average = BigDecimal.ZERO;

    private ScheduledExecutorService service;

    public StatisticsImpl()
    {
        LOGGER.info("Pool Initializing Started.");
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new DataProcessor(), DELAY, RUNNING_PERIOD, TimeUnit.SECONDS);
        LOGGER.info("Pool Initialized to run every second.");
    }

    @Override
    public Statistic getStatistic()
    {
        BigDecimal minAmount = getMin();
        BigDecimal maxAmount = getMax();
        int count = dataQueue.size();

        Statistic statistic = new Statistic(totalSum.doubleValue(), average.doubleValue(), maxAmount.doubleValue(), minAmount.doubleValue(), count);
        LOGGER.info("Statistics Returned : " + statistic);

        return statistic;
    }

    private BigDecimal getMin()
    {
        BigDecimal minAmount = BigDecimal.ZERO;
        Data minAmountData = minDataQueue.peek();

        if (minAmountData != null)
        {
            minAmount = minAmountData.getAmount();
        }
        return minAmount;
    }

    private BigDecimal getMax()
    {
        BigDecimal maxAmount = BigDecimal.ZERO;
        Data maxAmountData = maxDataQueue.peek();

        if (maxAmountData != null)
        {
            maxAmount = maxAmountData.getAmount();
        }
        return maxAmount;
    }

    @Override
    public void addData(Data data)
    {
        long expiryTimeInMilli = getExpiryTime();

        if (data.getTime() < expiryTimeInMilli)
        {
            LOGGER.info("Expired Data wont be added :" + data);
            throw new RuntimeException("Expired data can not be added!");
        }

        LOGGER.info("Adding data :" + data);
        processData(data);
    }

    private synchronized void processData(Data data)
    {
        addDataToQueues(data);

        while (hasQueueElementsAndIsTimeExpired())
        {
            removeDataFromQueues();
        }

        LOGGER.info("Status  : Sum : " + totalSum + " Average : " + average + " Max : " + getMax() +
                    " Min : " + getMin() + " Count : " + dataQueue.size());
    }

    private void addDataToQueues(Data data)
    {
        if (data != null)
        {
            dataQueue.add(data);
            totalSum = totalSum.add(data.getAmount());
            minDataQueue.add(data);
            maxDataQueue.add(data);
            average = totalSum.divide(BigDecimal.valueOf(dataQueue.size()), 2, RoundingMode.HALF_UP);
        }
    }

    private boolean hasQueueElementsAndIsTimeExpired()
    {
        return dataQueue.peek() != null && dataQueue.peek().getTime() < getExpiryTime();
    }

    private long getExpiryTime()
    {
        LocalDateTime givenSecondsAgo = LocalDateTime.now().minusSeconds(EXPIRY_SECONDS);
        ZonedDateTime zdt = givenSecondsAgo.atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    private void removeDataFromQueues()
    {
        Data removed = dataQueue.remove();
        totalSum = totalSum.subtract(removed.getAmount());
        minDataQueue.remove(removed);
        maxDataQueue.remove(removed);
        LOGGER.info("Removed expired data! " + removed);

        if (dataQueue.size() > 0)
        {
            average = totalSum.divide(BigDecimal.valueOf(dataQueue.size()), 2, RoundingMode.HALF_UP);
        } else
        {
            average = BigDecimal.ZERO;
        }
    }

    class DataProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processData();

            } catch (Exception e)
            {
                LOGGER.severe("Error occured during processing data " + e.getMessage());
            }
        }
    }

    private void processData()
    {
        processData(null);
    }

}
