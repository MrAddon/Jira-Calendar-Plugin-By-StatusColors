package it.com.atlassian.jira.ext.calendar;

import com.atlassian.jira.ext.calendar.HtmlCalendarConfiguration;
import com.atlassian.jira.functest.framework.FuncTestCase;
import com.atlassian.jira.functest.framework.locator.XPathLocator;
import com.atlassian.jira.issue.index.DocumentConstants;

import com.meterware.httpunit.HttpUnitOptions;

import org.hamcrest.CoreMatchers;

import static org.junit.Assert.assertThat;

public class TestCalendarPlugin extends FuncTestCase
{
    @Override
    protected void setUpTest()
    {
        super.setUpTest();
        administration.restoreData("TestCalendarPluginData.xml");
        HttpUnitOptions.setScriptingEnabled(true);
    }

    public void testProjectTabPanel()
    {
        navigation.gotoPage(generateHtmlServletUrl("042006", DocumentConstants.ISSUE_DUEDATE, "true", HtmlCalendarConfiguration.PROJECT_TAB_CONTEXT, "10000"));
        tester.assertTextPresent("April 2006");

        XPathLocator locator = new XPathLocator(tester, "//a[@class='version']");
        assertThat(locator.getText(), CoreMatchers.containsString("version 2"));
        assertThat(locator.getText(), CoreMatchers.containsString("Project: TST"));

        tester.assertTextPresent("Versions:");
        tester.assertLinkPresentWithText("On");

        // Don't display versions
        navigation.gotoPage(generateHtmlServletUrl("042006", DocumentConstants.ISSUE_DUEDATE, "false", HtmlCalendarConfiguration.PROJECT_TAB_CONTEXT, "10000"));
        tester.assertTextPresent("April 2006");

        locator = new XPathLocator(tester, "//a[@class='version']");
        assertEquals("", locator.getText()); // Because versions are turned off

        tester.assertTextPresent("Versions:");
        tester.assertLinkPresentWithText("On");

        // Try changing months
        navigation.gotoPage(generateHtmlServletUrl("052006", DocumentConstants.ISSUE_DUEDATE, "false", HtmlCalendarConfiguration.PROJECT_TAB_CONTEXT, "10000"));
        tester.assertTextPresent("May 2006");
    }

    public String generateHtmlServletUrl(String month, String dateFieldName, String displayVersions, String context, String key)
    {
        String url = "/rest/calendar-plugin/1.0/calendar/htmlcalendar.html?searchRequestId=10000&month=" + month
                + "&" + (HtmlCalendarConfiguration.PROJECT_TAB_CONTEXT.equals(context) ? "projectId" : "portletId") + "=" + key
                + "&dateFieldName=" + dateFieldName
                + "&displayVersions=" + displayVersions
                + "&numOfIssueIcons=10&context=" + context;

        return url;
    }
}