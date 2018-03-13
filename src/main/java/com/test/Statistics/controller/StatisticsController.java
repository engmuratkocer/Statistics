package com.test.Statistics.controller;

import com.test.Statistics.domain.Data;
import com.test.Statistics.domain.Statistic;
import com.test.Statistics.service.Statistics;
import com.test.Statistics.service.StatisticsImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

@RestController
public class StatisticsController
{
    Statistics statistics = new StatisticsImpl();

    @RequestMapping(value = "/statistics", method = RequestMethod.GET)
    public Statistic getStatistic()
    {
        return statistics.getStatistic();
    }

    @RequestMapping(value = "/transactions", method = RequestMethod.POST)
    public void addData(@RequestParam(required = false, value = "amount") double amount, @RequestParam(value = "timestamp") Long timestamp, HttpServletResponse response)
    {
        try
        {
            statistics.addData(new Data(new BigDecimal(amount), timestamp));
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (Exception e)
        {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

    }
}