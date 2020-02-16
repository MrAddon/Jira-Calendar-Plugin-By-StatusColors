package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.ext.calendar.ParameterUtils;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.version.Version;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a month of Jira Issues and Jira Versions divided among the weeks and
 * days of a given month.
 */
public class MonthEvents
{
    private static final Logger log = Logger.getLogger(MonthEvents.class.getName());

    private DayEvents[] dayEventList;
    private WeekEvents[] weekEventList;

    private int paddingDaysStart;

    private final Date previousMonthDate;
    private final Date monthStartDate;
    private final Date nextMonthDate;

    /**
     * @param issueList   The issues for this month
     * @param versionList The versions for this motnh
     * @param monthStart  The first day of the month
     */
    public MonthEvents(List issueList, List versionList, Calendar monthStart, DateFieldProvider dateFieldProvider)
    {
        this.monthStartDate = monthStart.getTime();
        this.previousMonthDate = ParameterUtils.cloneAddMonths(monthStart, -1).getTime();
        this.nextMonthDate = ParameterUtils.cloneAddMonths(monthStart, 1).getTime();

        // Wrote my own modulo method because Java's native implementation returns negative numbers?!?
        paddingDaysStart = modulo(monthStart.get(Calendar.DAY_OF_WEEK) - (monthStart.getFirstDayOfWeek()), 7) + 1;

        Calendar calendarStart = (Calendar) monthStart.clone();
        calendarStart.add(Calendar.DAY_OF_YEAR, -paddingDaysStart);

        final int weeksInMonth = monthStart.getActualMaximum(Calendar.WEEK_OF_MONTH);

        dayEventList = createDays(calendarStart, weeksInMonth * 7);

        for (Iterator it = issueList.iterator(); it.hasNext();)
        {
            Issue issue = (Issue) it.next();
            Date date = dateFieldProvider.getDate(issue);
            Calendar issueDueCal = GregorianCalendarFactory.constructCalendar(date);
            int dayOfMonth = getDayOfMonth(date);

            int dayEventIndex = dayOfMonth + paddingDaysStart - 2;
            if(isSameMonth(monthStart, issueDueCal) && ((dayEventIndex) < dayEventList.length))
            {
                dayEventList[dayEventIndex].addIssue(issue);
            }
            // JCAL-93 - Removed the error message logging since it will not affect the plugin functionality in any way.
        }

        for (Iterator it = versionList.iterator(); it.hasNext();)
        {
            Version version = (Version) it.next();
            Calendar versionRelCal = GregorianCalendarFactory.constructCalendar(version.getReleaseDate());
            int dayOfMonth = getDayOfMonth(version.getReleaseDate());

            if(isSameMonth(monthStart, versionRelCal) && ((dayOfMonth + paddingDaysStart -2) < dayEventList.length))
            {
                dayEventList[dayOfMonth + paddingDaysStart - 2].addVersion(version);
            }
            // JCAL-93 - Removed the error message logging since it will not affect the plugin functionality in any way.
        }

        weekEventList = new WeekEvents[weeksInMonth];
        for (int i = 0; i < weekEventList.length; i++)
        {
            weekEventList[i] = new WeekEvents(i);
            for (int j = 0; j < 7; j++)
            {
                weekEventList[i].setDay(j, dayEventList[(i * 7) + j]);
            }
        }
    }

    /**
     * @param date
     * @return the Day of the month this date falls on.
     */
    public int getDayOfMonth(Date date)
    {
        return GregorianCalendarFactory.constructCalendar(date).get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Creates and initializes an array of DayEvents
     *
     * @param startDay The date of the first DayEvent in the array
     * @param noOfDays The total number of DayEvents to be in the array
     * @return the array of DayEvents
     */
    private static DayEvents[] createDays(Calendar startDay, int noOfDays)
    {
        DayEvents[] dayList = new DayEvents[noOfDays];

        for (int i = 0; i < noOfDays; i++)
        {
            Calendar dayCalendar = (Calendar) startDay.clone();
            dayCalendar.add(Calendar.DAY_OF_YEAR, i + 1);
            dayList[i] = new DayEvents(dayCalendar);
        }

        return dayList;
    }

    /**
     * Compares two dates
     *
     * @param d1
     * @param d2
     * @return true if the dates are in the same calendar year.
     */
    private static boolean isSameYear(Calendar d1, Calendar d2)
    {
        return (d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR));
    }

    /**
     * Compares two dates
     *
     * @param d1
     * @param d2
     * @return true if the dates are in the same month
     */
    private static boolean isSameMonth(Calendar d1, Calendar d2)
    {
        return isSameYear(d1, d2) && (d1.get(Calendar.MONTH) == d2.get(Calendar.MONTH));
    }

    /**
     * Compares two dates
     *
     * @param d1
     * @param d2
     * @return true if both dates are on the same day.
     */
    public static boolean isSameDay(Calendar d1, Calendar d2)
    {
        return isSameYear(d1, d2) && (d1.get(Calendar.DAY_OF_YEAR) == d2.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * @return the array of DayEvents for this Month
     */
    public DayEvents[] getDayEventList()
    {
        return dayEventList;
    }

    /**
     * @return the array of WeekEvents for this Month
     */
    public WeekEvents[] getWeekEventList()
    {
        return weekEventList;
    }

    public Date getMonthStartDate()
    {
        return monthStartDate;
    }

    public Date getNextMonthDate()
    {
        return nextMonthDate;
    }

    public Integer getNextMonthOfYear()
    {
        return getMonthOfYear(nextMonthDate);
    }

    public Date getPreviousMonthDate()
    {
        return previousMonthDate;
    }

    public Integer getPreviousMonthOfYear()
    {
        return getMonthOfYear(previousMonthDate);
    }

    /**
     * @return which month of the year this represents
     */
    public Integer getMonthOfYear()
    {
        return getMonthOfYear(monthStartDate);
    }

    public Integer getMonthOfYear(Date date)
    {
        return new Integer(GregorianCalendarFactory.constructCalendar(date).get(Calendar.MONTH));
    }

    // Made my own modulo method because Java's returns negative numbers!?!
    private int modulo(int x, int y)
    {
        int result = x%y;
        if(x < 0)
        {
            return result + y;
        }
        else
        {
            return result;
        }

    }
}
