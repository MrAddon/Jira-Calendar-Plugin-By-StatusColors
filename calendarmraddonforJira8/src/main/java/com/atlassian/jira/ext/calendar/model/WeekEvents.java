package com.atlassian.jira.ext.calendar.model;

public class WeekEvents
{
    private DayEvents[] dayList = new DayEvents[7];
    private int weekOfMonth;

    public WeekEvents(int weekOfMonth)
    {
        this.weekOfMonth = weekOfMonth;
    }

    public void setDay(int i, DayEvents day)
    {
        dayList[i] = day;
    }

    public DayEvents getDay(int i)
    {
        return dayList[i];
    }

    public DayEvents[] getDayList()
    {
        return dayList;
    }

    public int getWeekOfMonth()
    {
        return weekOfMonth;
    }
}
