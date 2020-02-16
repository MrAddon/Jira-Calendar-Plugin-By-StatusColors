package com.atlassian.jira.ext.calendar.page;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

public class CalendarTabPage
{
    private final PageElement tabContents;

    public CalendarTabPage(PageElement tabContents)
    {
        this.tabContents = tabContents;
    }

    public boolean isIssueOnCalendar(final String issueKey)
    {
        return tabContents.find(By.className("issue")).getAttribute("href").toString().contains(issueKey);
    }
}
