package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.priority.Priority;

import java.util.Comparator;
import java.util.Date;

public class IssueComparator implements Comparator<Issue>
{
	private DateFieldProvider dateFieldProvider;

    public IssueComparator(String dateFieldName) {
		this.dateFieldProvider = new DateFieldProvider(dateFieldName);
	}

	/**
     * Compares two issues and orders them based on the due date and then based on Id.
     */
    public int compare(Issue issue1, Issue issue2)
    {
        if(issue1 == issue2)
        {
            return 0;
        }

        Date issue1Date = dateFieldProvider.getDate(issue1);
        Date issue2Date = dateFieldProvider.getDate(issue2);

        if (null == issue1Date && null != issue2Date) return -1;
        if (null != issue1Date && null == issue2Date) return 1;
        if (null != issue1Date && null != issue2Date && issue1Date != issue2Date)
        {
            int dueDateComparison = issue1Date.compareTo(issue2Date);
            if(dueDateComparison != 0)
            {
                return dueDateComparison;
            }
        }

        Priority issue1Priority = issue1.getPriorityObject();
        Priority issue2Priority = issue2.getPriorityObject();

        if (null == issue1Priority && null != issue2Priority) return -1;
        if (null != issue1Priority && null == issue2Priority) return 1;
        if (null != issue1Priority && null != issue2Priority && issue1Priority != issue2Priority)
        {
            int issueComparison = issue1Priority.compareTo(issue2Priority);
            if(issueComparison != 0)
            {
                return issueComparison;
            }
        }

        return issue1.getId().compareTo(issue2.getId());
    }



}
