package com.atlassian.jira.ext.calendar;

import com.atlassian.core.util.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ParameterUtils
{
    static final DateFormat format = new SimpleDateFormat("MMyyyy");

    public static boolean getBoolean(String booleanStr)
    {
        if(booleanStr == null) {
            return true;
        }

        return Boolean.valueOf(booleanStr).booleanValue();
    }

    public static Long getLong(String paramStr)
    {
        try
        {
            return new Long(paramStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static Long getDuration(String durStr)
    {
        if (durStr != null)
        {
            try
            {
            	return new Long(DateUtils.getDuration(durStr) * DateUtils.SECOND_MILLIS);
            }
            catch (Exception e)
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    public static Date getMonth(String monthStr)
    {
        try {
            return format.parse(monthStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date endOfMonth(Date month)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));
        return calendar.getTime();
    }

    public static String getMonthStr(Date month)
    {
        return format.format(month);
    }

    public static Calendar cloneAddMonths(Calendar cal, int months)
    {
        Calendar newMonthCal = (Calendar) cal.clone();
        newMonthCal.add(Calendar.MONTH, months);
        return newMonthCal;
    }

    public static boolean isProject(String value)
    {
        return isBaseMethod(value, "project-");
    }

    public static boolean isFilter(String value)
    {
        return isBaseMethod(value, "filter-");
    }

    public static Long getProjectId(String value)
    {
        if(isProject(value))
        {
           return new Long(value.substring(8));
        }
        return null;
    }

    public static Long getFilterId(String value)
    {
        if (isFilter(value))
        {
            return new Long(value.substring(7));
        }
        return null;
    }

    private static boolean isBaseMethod(String value, String prefix)
    {
        return ((value != null) && (value.startsWith(prefix)));
    }
}