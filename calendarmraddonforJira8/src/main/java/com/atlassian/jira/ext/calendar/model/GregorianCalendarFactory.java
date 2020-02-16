package com.atlassian.jira.ext.calendar.model;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public final class GregorianCalendarFactory
{
    public static Calendar constructCalendar()
    {
        Calendar cal = new GregorianCalendar();
        cal.setMinimalDaysInFirstWeek(1);
        return cal;
    }

    public static Calendar constructCalendar(Date date)
    {
        Calendar cal = constructCalendar();
        cal.setTime(date);
        return cal;
    }
}
