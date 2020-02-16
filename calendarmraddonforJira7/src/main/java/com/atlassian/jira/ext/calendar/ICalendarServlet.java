package com.atlassian.jira.ext.calendar;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge;
import com.atlassian.jira.ext.calendar.model.CalendarFactory;
import com.atlassian.jira.ext.calendar.model.CalendarSearchRequest;
import com.atlassian.jira.ext.calendar.model.VersionDelegator;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for generating ICal format files containg the issues and--maybe--versions
 * for a given Search or Project.
 */
public class ICalendarServlet extends HttpServlet
{

    private static final Logger LOG = Logger.getLogger(ICalendarServlet.class);

    private final JiraAuthenticationContext jiraAuthenticationContext;

    private final PermissionManager  permissionManager;

    private final ProjectManager projectManager;

    private final SearchRequestService searchRequestService;

    private final OfBizDelegator ofBizDelegator;

    private final VersionManager versionManager;

    private final SearchServiceBridge searchService;
    
    private final CustomFieldManager customFieldManager;

    public ICalendarServlet(JiraAuthenticationContext jiraAuthenticationContext, PermissionManager permissionManager, ProjectManager projectManager, SearchRequestService searchRequestService, OfBizDelegator ofBizDelegator, VersionManager versionManager, SearchServiceBridge searchService, CustomFieldManager customFieldManager)
    {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.searchRequestService = searchRequestService;
        this.ofBizDelegator = ofBizDelegator;
        this.versionManager = versionManager;
        this.searchService = searchService;
        this.customFieldManager = customFieldManager;
    }

    /**
     *
     * @param req Request parameters expected to be found in the request object (only one of
     * searchRequestId and projectId are expected to be found in the request):
     * <dl>
     * <dt>searchRequestId (One of searchRequestId and projectId)</dt>
     * <dd>The id of the SearchRequest that is to be used to find the issues displayed in the calendar</dd>
     *
     * <dt>projectId (One of searchRequestId and projectId)</dt>
     * <dd>The id of the Project whose issues are to be used to generate the calendar</dd>
     *
     * <dt>startOffset (Optional)</dt>
     * <dd>The offset from the current date in  milliseconds from now for the start of the calendar</dd>
     *
     * <dt>endOffset (Optional)</dt>
     * <dd>The offset from the current date in  milliseconds from now for the end of the calendar</dd>
     *
     * <dt>showVersions(Optional)</dt>
     * <dd>"true" or "false", whether project versions are to be displayed in the generated calendar</dd>
     * </dl>
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        ApplicationUser user = jiraAuthenticationContext.getUser();

        Long searchId = ParameterUtils.getLong(req.getParameter("searchRequestId"));
        Long projectId = ParameterUtils.getLong(req.getParameter("projectId"));
        Long startOffset = ParameterUtils.getDuration(req.getParameter("startOffset")) != null ? 
        		-ParameterUtils.getDuration(req.getParameter("startOffset")) : null;
        Long endOffset = ParameterUtils.getDuration(req.getParameter("endOffset"));
        String dateFieldName = req.getParameter("dateFieldName");
        boolean showVersions = ParameterUtils.getBoolean(req.getParameter("showVersions"));

        CalendarSearchRequest calSearch;

        try {
            calSearch = new CalendarSearchRequest(user, searchId, projectId, dateFieldName, startOffset, endOffset, new VersionDelegator(ofBizDelegator, versionManager), permissionManager, projectManager, searchRequestService, searchService, customFieldManager);
        } catch(Exception e) {
            LOG.error("Exception occcured creating ICal Calendar", e);
            return;
        }

        CalendarFactory factory = new CalendarFactory(calSearch, dateFieldName);
        Calendar calendar = factory.createICalendarFromSearch(showVersions);

        CalendarOutputter calWriter = new CalendarOutputter(true);

        res.setContentType("text/calendar");
        try
        {
            calWriter.output(calendar, res.getWriter());
        }
        catch (ValidationException ve)
        {
            LOG.error("ICalendarServlet.service(HttpServletRequest, HttpServletReponse) ValidationException encountered while writing calendar.", ve);
        }
    }
}
