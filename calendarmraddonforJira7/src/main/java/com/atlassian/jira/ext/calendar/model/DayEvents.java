package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.version.Version;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A list of issues and versions that are due on the given day.
 */
public class DayEvents
{
    private final Calendar today;
    private List issueList;
    private List versionList;


    public DayEvents(Calendar today)
    {
        this.today = today;
        issueList = new ArrayList();
        versionList = new ArrayList();
    }

    /**
     * Adds an issue to the list of issues for this day.
     *
     * @param issue
     */
    public void addIssue(Issue issue)
    {
        issueList.add(issue);
    }

    /**
     * Adds a version to the the list of versions for this day.
     *
     * @param version
     */
    public void addVersion(Version version)
    {
        versionList.add(version);
    }

    /**
     * @return a calendar object corresponding to this day.
     */
    public Calendar getToday()
    {
        return today;
    }

    /**
     * @return a Date object corresponding to this day.
     */
    public Date getTodayDate()
    {
        return today.getTime();
    }

    /**
     * @return a list of events that are due on this day.
     */
    public List getIssues()
    {
        return issueList;
    }

    public List getFirstXIssues(int x)
    {
        return issueList.subList(0, Math.min(x, issueList.size()));
    }

    /**
     * @return which day of the month this day is.
     */
    public int getDayOfMonth()
    {
        return today.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * @return which month of the year this day is in.
     */
    public int getMonthOfYear()
    {
        return today.get(Calendar.MONTH);
    }

    public Integer getDayOfWeek()
    {
        return new Integer(today.get(Calendar.DAY_OF_WEEK));
    }

    /**
     * @return the number of events that are due on this day.
     */
    public Integer getNoOfIssues()
    {
        return new Integer(issueList.size());
    }

    /**
     * @return a list of versions that are due on this day.
     */
    public List getVersions()
    {
        return versionList;
    }

    /**
     * @return the number of versions that are due on this day.
     */
    public Integer getNoOfVersions()
    {
        return new Integer(versionList.size());
    }

}
