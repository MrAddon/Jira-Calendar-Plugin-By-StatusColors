package com.atlassian.jira.ext.calendar.page;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Conditions.and;

public class ProjectPage extends AbstractJiraPage
{
    private final String projectKey;

    @Inject
    private PageBinder pageBinder;

    @ElementBy(id="project-tab")
    private PageElement tabContents;

    @ElementBy(id="com.atlassian.jira.ext.calendar:issuecalendar-panel-panel")
    private PageElement calendarTab;

    public ProjectPage(String projectKey)
    {
        this.projectKey = projectKey;
    }

    @Override
    public TimedCondition isAt()
    {
        return and(tabContents.timed().isPresent(), tabContents.timed().hasAttribute("data-project-key", projectKey));
    }

    @Override
    public String getUrl()
    {
        return "/browse/" + projectKey;
    }

    public CalendarTabPage getCalendarTab()
    {
        calendarTab.click();
        final PageElement issuesCalendar = tabContents.find(By.className("issues-calendar"), TimeoutType.PAGE_LOAD);
        Poller.waitUntilTrue(issuesCalendar.timed().isPresent());
        return new CalendarTabPage(tabContents);
    }
}
