package at.com.atlassian.jira.ext.calendar;

import com.atlassian.integrationtesting.runner.restore.RestoreOnce;
import com.atlassian.jira.ext.calendar.backdoor.CalendarControl;
import com.atlassian.jira.functest.framework.RestoreBlankInstance;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.CreateUser;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.jira.pageobjects.dialogs.quickedit.EditIssueDialog;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.test.categories.OnDemandAcceptanceTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertTrue;

/**
 * Test for JIRA Calendar Plugin. <a href="https://plugins.atlassian.com/plugin/details/293">JIRA Calendar</a>
 *
 * @since v6.2
 */
@Category (OnDemandAcceptanceTest.class)
public class TestJiraCalendar extends BaseJiraWebTest
{
    public static final String DEVELOPER = "developer";
    public static final String TEST_PROJECT_KEY = "TSTC";

    private final CalendarControl calendarControl;

    private static String issueKey;
    private static Long projectId;

    public TestJiraCalendar()
    {
        calendarControl = new CalendarControl(jira.environmentData());
    }

    @Before
    public void setUp()
    {
        backdoor.restoreBlankInstance();
        projectId = jira.backdoor().project().addProject("test calendar", TEST_PROJECT_KEY, "admin");
        issueKey = jira.backdoor().issues().createIssue(TEST_PROJECT_KEY, "This is a test issue for JIRA calendars").key();
    }

    @Test
    public void testIfCalendarIsUpAndRunning()
    {
        final ViewIssuePage page = jira.getPageBinder().navigateToAndBind(ViewIssuePage.class, issueKey);
        final EditIssueDialog dialog = page.editIssue();
        dialog.setDueDate(format(new Date(), "dd/MMM/yy"));
        dialog.submit();

        final String calendarServletResponse = calendarControl.getCalendarForProjectAndField(projectId, "duedate");
        assertTrue("The calendar servlet response did not contain the VCALENDAR start tag", StringUtils.contains(calendarServletResponse, "BEGIN:VCALENDAR"));
        assertTrue("The issue with a due date did not appear on the calendar", StringUtils.contains(calendarServletResponse, issueKey));
    }

    private String format(Date date, String format)
    {
        final DateFormat dateFormat = new SimpleDateFormat(format);
        // UTC is the time zone of our OnDemand test data, which is important to explicitly
        // set given our test might be running on a computer with a different timezone to that
        // of the OnDemand instance we're testing against.
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }
}
