package com.test.Statistics.service;

import com.test.Statistics.domain.Data;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.test.Statistics.service.StatisticsImpl.*;
import static org.junit.Assert.assertEquals;

public class StatisticsImplTest
{

    private static final Logger LOGGER = Logger.getLogger(StatisticsImplTest.class.getName());

    private Statistics statistics;
    private Random random = new Random();

    @Before
    public void setUp()
    {
        EXPIRY_SECONDS = 1;
        statistics = new StatisticsImpl();
    }

    @After
    public void tearDown() throws InterruptedException
    {
        //wait expire time to be sure queues get empty
        waitTimeToExpire();
    }

    @Test
    public void sumAndAverageShouldUsedAllDataAddedBeforeTimeExpiry() throws InterruptedException
    {
        //GIVEN
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal avg;
        BigDecimal amount = BigDecimal.valueOf(10);
        BigDecimal count = BigDecimal.valueOf(30);

        //WHEN
        total = addData(total, amount, count);
        avg = total.divide(count, 2, RoundingMode.HALF_UP);

        //THEN
        assertEquals(total.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getSum()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(avg.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getAvg()).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void sumAndAverageShouldNotUserExpiredData() throws InterruptedException
    {
        //GIVEN
        BigDecimal total;
        BigDecimal avg;
        BigDecimal amount = BigDecimal.valueOf(10);
        BigDecimal count = BigDecimal.valueOf(30);

        //WHEN
        total = addDataBeforeAndAfterExpireTime(amount, count);
        avg = total.divide(count, 2, RoundingMode.HALF_UP);

        //THEN
        assertEquals(total.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getSum()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(avg.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getAvg()).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void shouldUseLastlyAddedDataToGetMinMaxAfterTimeExpiry() throws InterruptedException
    {
        //GIVEN
        BigDecimal max = BigDecimal.valueOf(100);
        BigDecimal min = BigDecimal.valueOf(1);

        //WHEN
        addMinMax(max, min);
        waitTimeToExpire();
        max = BigDecimal.valueOf(99);
        min = BigDecimal.valueOf(1);
        addMinMax(max, min);

        //THEN
        assertEquals(max.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getMax()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(min.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getMin()).setScale(2, RoundingMode.HALF_UP));

    }

    @Test
    public void shouldUseAllDataToGetMinMaxBeforeTimeExpiry() throws InterruptedException
    {
        //GIVEN
        BigDecimal max = BigDecimal.valueOf(100);
        BigDecimal min = BigDecimal.valueOf(1);

        //WHEN
        addMinMax(max, min);

        //THEN
        assertEquals(max.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getMax()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(min.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getMin()).setScale(2, RoundingMode.HALF_UP));
    }


    @Test
    public void shouldCalculateTotalAndAverageSumUnderContinousLoadWithoutExpire() throws InterruptedException
    {
        //GIVEN
        EXPIRY_SECONDS = 5;
        int numberOfRuns = 100;
        int pauseTime = 10;
        BigDecimal max;
        BigDecimal min;
        BigDecimal count;
        BigDecimal avg = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        AtomicBoolean goOnAddingData = new AtomicBoolean(true);

        //WHEN
        loadDataConcurrently(numberOfRuns, pauseTime, goOnAddingData);
        //stop loading operation
        goOnAddingData.set(false);

        //calculate results at the queues
        total = getTotalAmount(total);
        count = new BigDecimal(dataQueue.size());
        avg = getAverage(count, avg, total);
        min = getMin();
        max = getMax();

        //THEN
        LOGGER.info("Final Results : " + statistics.getStatistic().toString());
        assertAllResults(max, min, count, avg, total);
    }

    @Test
    public void shouldCalculateTotalAndAverageSumUnderContinousLoadAfterEveryExpire() throws InterruptedException
    {
        //GIVEN
        EXPIRY_SECONDS = 1;
        int numberOfRuns = 1000;
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal count;
        BigDecimal avg = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int pauseTime = 10;
        AtomicBoolean goOnAddingData = new AtomicBoolean(true);

        //WHEN
        loadDataConcurrently(numberOfRuns, pauseTime, goOnAddingData);
        //stop loading operation
        goOnAddingData.set(false);

        //calculate results at the queues
        total = getTotalAmount(total);
        count = new BigDecimal(dataQueue.size());
        avg = getAverage(count, avg, total);
        min = getMin();
        max = getMax();

        //THEN
        LOGGER.info("Final Results : " + statistics.getStatistic().toString());
        assertAllResults(max, min, count, avg, total);
    }

    private BigDecimal getAverage(BigDecimal count, BigDecimal avg, BigDecimal total)
    {
        if (count.compareTo(BigDecimal.ZERO) == 1)
        {
            avg = total.divide(count, 2, RoundingMode.HALF_UP);
        }
        return avg;
    }

    private BigDecimal getMin()
    {
        BigDecimal min = BigDecimal.ZERO;

        if (minDataQueue.peek() != null)
            min = minDataQueue.peek().getAmount();
        return min;
    }

    private BigDecimal getMax()
    {
        BigDecimal max = BigDecimal.ZERO;

        if (maxDataQueue.peek() != null)
            max = maxDataQueue.peek().getAmount();
        return max;
    }

    private void assertAllResults(BigDecimal max, BigDecimal min, BigDecimal count, BigDecimal avg, BigDecimal total)
    {
        assertEquals(total.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getSum()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(avg.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getAvg()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(max.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getMax()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(min.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getMin()).setScale(2, RoundingMode.HALF_UP));
        assertEquals(count.setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(statistics.getStatistic().getCount()).setScale(2, RoundingMode.HALF_UP));
    }

    private long getCurrentTime()
    {
        LocalDateTime sixtySecondsAgo = LocalDateTime.now();
        ZonedDateTime zdt = sixtySecondsAgo.atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    private long getExpiryTime()
    {
        LocalDateTime givenSecondsAgo = LocalDateTime.now().minusSeconds(EXPIRY_SECONDS);
        ZonedDateTime zdt = givenSecondsAgo.atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    private BigDecimal addDataBeforeAndAfterExpireTime(BigDecimal amount, BigDecimal count) throws InterruptedException
    {
        BigDecimal total;
        for (int i = 0; i < count.intValue(); i++)
        {
            statistics.addData(new Data(amount, getCurrentTime()));
        }
        waitTimeToExpire();

        total = BigDecimal.ZERO;
        amount = BigDecimal.valueOf(5);

        total = addData(total, amount, count);
        return total;
    }

    private BigDecimal addData(BigDecimal total, BigDecimal amount, BigDecimal count)
    {
        for (int i = 0; i < count.intValue(); i++)
        {
            statistics.addData(new Data(amount, getCurrentTime()));
            total = total.add(amount);
        }
        return total;
    }

    private void waitTimeToExpire() throws InterruptedException
    {
        TimeUnit.SECONDS.sleep(EXPIRY_SECONDS);
    }

    private void addMinMax(BigDecimal max, BigDecimal min)
    {
        statistics.addData(new Data(min, getCurrentTime()));
        statistics.addData(new Data(max, getCurrentTime()));
    }


    private BigDecimal getTotalAmount(BigDecimal total)
    {
        for (Data data : dataQueue)
        {
            total = total.add(data.getAmount());
        }
        return total;
    }

    private void loadDataConcurrently(int numberOfRuns, int pauseTime, AtomicBoolean goOnAddingData) throws InterruptedException
    {
        Thread thread = randamDataLoaderThread(goOnAddingData);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < numberOfRuns; i++)
        {
            executorService.submit(thread);
            TimeUnit.MILLISECONDS.sleep(pauseTime);
        }
    }

    private Thread randamDataLoaderThread(AtomicBoolean goOnAddingData)
    {
        return new Thread(() -> {
            BigDecimal amount = new BigDecimal(random.nextDouble() * 100);
            Data data = new Data(amount, getCurrentTime());
            if (goOnAddingData.get())
            {
                statistics.addData(data);
            } else
            {
                throw new RuntimeException("Data Addition Stopped!");
            }
        });
    }

}
