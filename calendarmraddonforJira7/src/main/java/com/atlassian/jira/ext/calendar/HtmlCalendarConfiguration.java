package com.atlassian.jira.ext.calendar;

import com.atlassian.jira.ext.calendar.model.GregorianCalendarFactory;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.project.browse.BrowseContext;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpSession;
import java.util.Calendar;

public class HtmlCalendarConfiguration
{
    private static final Logger LOG = Logger.getLogger(HtmlCalendarConfiguration.class);

    private Long filterId;
    private Long projectId;
    private Calendar monthStart;
    private Long portletId;
    private String dateFieldName;
    private boolean displayVersions;
    private Long numOfIssueIcons;
    private String context;

    public static final String PROJECT_TAB_CONTEXT = "projectTab";

    private static final String START_DATE_VAR = "startDate";
    private static final String DISPLAY_VER_VAR = "displayVersions";

    private static final Long DEFAULT_NUM_ISSUES = 10L;

    public HtmlCalendarConfiguration(BrowseContext browseContext, HttpSession session)
            throws Exception
    {
        this(null,
                getProjId(browseContext),
                getBrowseMonth(session, getProjId(browseContext), PROJECT_TAB_CONTEXT),
                getProjId(browseContext),
                DocumentConstants.ISSUE_DUEDATE,
                getDisplayVersions(session, getProjId(browseContext), PROJECT_TAB_CONTEXT),
                DEFAULT_NUM_ISSUES,
                PROJECT_TAB_CONTEXT);
    }

    public HtmlCalendarConfiguration(Long searchId, Long projectId, Calendar monthStart, Long portletId,
                                        String dateFieldName, boolean showVersions, Long numOfIssueIcons, String context)
    {
        this.filterId = searchId;
        this.projectId = projectId;
        this.monthStart = null == monthStart ? getCurrentMonth() : monthStart;
        this.portletId = portletId;
        this.dateFieldName = dateFieldName;
        this.displayVersions = showVersions;
        this.numOfIssueIcons = numOfIssueIcons;
        this.context = context;
    }

    

    public static Calendar getCurrentMonth()
    {
        Calendar currentCal = GregorianCalendarFactory.constructCalendar();
        Calendar monthStartCal = GregorianCalendarFactory.constructCalendar();
        monthStartCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), monthStartCal.getMinimum(Calendar.DAY_OF_MONTH));
        monthStartCal.set(Calendar.AM_PM, Calendar.AM);
        monthStartCal.set(Calendar.HOUR, monthStartCal.getMinimum(Calendar.HOUR));
        monthStartCal.set(Calendar.MINUTE, monthStartCal.getMinimum(Calendar.MINUTE));
        monthStartCal.set(Calendar.SECOND, monthStartCal.getMinimum(Calendar.SECOND));
        monthStartCal.set(Calendar.MILLISECOND, monthStartCal.getMinimum(Calendar.MILLISECOND));

        return monthStartCal;
    }

    public Long getFilterId()
    {
        return filterId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public Calendar getMonthStart()
    {
        return monthStart;
    }

    public Long getPortletId()
    {
        return portletId;
    }

    public String getDateFieldName()
    {
		return dateFieldName;
	}

	public boolean isDisplayVersions()
    {
        return displayVersions;
    }

    public Long getNumOfIssueIcons()
    {
        return numOfIssueIcons;
    }

    public static String getMonthSessionKey(Long key, String context, String variable)
    {
       return "com.atlassian.jira.ext.calendar." + context + "." + key + "." + variable;
    }

    public String getContext()
    {
        return context;
    }

    private static boolean getDisplayVersions(HttpSession sess, Long key, String context)
    {
        Boolean displayVersions = null;
        try
        {
            displayVersions = (Boolean) sess.getAttribute(getMonthSessionKey(key, context, DISPLAY_VER_VAR));
        }
        catch(Exception e)
        {
            LOG.error("HtmlCalendarConfiguration.getDisplayVersions(HttpSession, Long, String):boolean", e);
        }

        return displayVersions == null || displayVersions;
    }

    public static Calendar getBrowseMonth(HttpSession session, Long key, String context)
    {
        Calendar startMonthCal = null;

        try
        {
            startMonthCal = (Calendar) session.getAttribute(getMonthSessionKey(key, context, START_DATE_VAR));
        }
        catch(Exception e)
        {
            LOG.error("HtmlCalendarConfiguration.getBrowseMonth(HttpSession, Long, String):Calendar", e);
        }

        if(startMonthCal == null) {
            startMonthCal = getCurrentMonth();
        }

        return startMonthCal;
    }

    public static void setDisplayVersions(HttpSession session, Long key, boolean displayVersions, String context)
    {
        session.setAttribute(HtmlCalendarConfiguration.getMonthSessionKey(key, context, HtmlCalendarConfiguration.DISPLAY_VER_VAR), displayVersions);
    }

    public static void setBrowseMonth(HttpSession session, Long key, Calendar monthStartCal, String context)
    {
        session.setAttribute(HtmlCalendarConfiguration.getMonthSessionKey(key, context, HtmlCalendarConfiguration.START_DATE_VAR), monthStartCal);
    }

    private static Long getProjId(BrowseContext browseContext) throws Exception
    {
        return browseContext.getProject().getId();
    }
}
