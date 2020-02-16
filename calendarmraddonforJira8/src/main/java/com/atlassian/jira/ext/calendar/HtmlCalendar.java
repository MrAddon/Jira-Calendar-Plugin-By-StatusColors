package com.atlassian.jira.ext.calendar;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.ext.calendar.model.CalendarFactory;
import com.atlassian.jira.ext.calendar.model.CalendarSearchRequest;
import com.atlassian.jira.ext.calendar.model.GregorianCalendarFactory;
import com.atlassian.jira.ext.calendar.model.InvalidFilterSearchRequestException;
import com.atlassian.jira.ext.calendar.model.InvalidProjectSearchRequestException;
import com.atlassian.jira.ext.calendar.model.MonthEvents;
import com.atlassian.jira.ext.calendar.model.VersionDelegator;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraVelocityUtils;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpSession;

/**
 * A utility class shared between the HtmlCalendarServlet 
 * for generating the map of parameters required by the Velocity template to display
 * the Issue Calendar.
 */
public class HtmlCalendar
{
    private static final Logger log = Logger.getLogger(HtmlCalendar.class.getName());

    protected final JiraAuthenticationContext authenticationContext;

    protected final VersionDelegator versionDelegator;

    protected final PermissionManager permissionManager;

    protected final ConstantsManager constantsManager;

    protected final ProjectManager projectManager;

    protected final SearchRequestService searchRequestService;

    protected final SearchServiceBridge searchService;
    
    protected final CustomFieldManager customFieldManager;

    public HtmlCalendar(JiraAuthenticationContext jiraAuthenticationContext, VersionDelegator versionDelegator, PermissionManager permissionManager, ConstantsManager constantsManager, ProjectManager projectManager, SearchRequestService searchRequestService, SearchServiceBridge searchService, CustomFieldManager customFieldManager)
    {
        this.authenticationContext = jiraAuthenticationContext;
        this.versionDelegator = versionDelegator;
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.projectManager = projectManager;
        this.searchRequestService = searchRequestService;
        this.searchService = searchService;
        this.customFieldManager = customFieldManager;
    }

    public Map<String, Object> getParameters(BrowseContext browseContext, HttpSession sess)
    {
        Map<String, Object> params = getBasicParameters();
        
        try {
            HtmlCalendarConfiguration config = new HtmlCalendarConfiguration(browseContext, sess);
            params.putAll(getCalendarParameters(config));
        }
        catch (PermissionException e)
        {
            registerError(params, "portlet.issuecalendar.error.permissionException",
                            "Permission exception encountered when trying to configure calendar from browser", e);
        }
        catch (Exception e)
        {
            registerError(params, "portlet.issuecalendar.error.browserException",
                            "Exception encountered when trying to configure calendar from browser", e);
        }
        return params;
    }

    public Map<String, Object> getParameters(HtmlCalendarConfiguration config)
    {
        Map<String, Object> params = getBasicParameters();
        params.putAll(getCalendarParameters(config));
        return params;
    }

    private Map<String, Object> getBasicParameters()
    {
        Map<String, Object> params = JiraVelocityUtils.getDefaultVelocityParams(null, authenticationContext);
        // Some universally useful params
        params.put("loggedin", authenticationContext.isLoggedInUser());
        params.put("i18n", authenticationContext.getI18nHelper());
        Date today = new Date();
        params.put("todayMonthStr", ParameterUtils.getMonthStr(today));
        params.put("todayDate", today);
        params.put("todayCal", GregorianCalendarFactory.constructCalendar());
        params.put("outlookDate", authenticationContext.getOutlookDate());
        params.put("priorities", constantsManager.getPriorityObjects());
        params.put("searchService", searchService);

        return params;
    }

    /**
     * @return A map including most of the parameters (except for HttpServletRequest "req")
     * required to render the velocity template.
     */
    private Map<String, Object> getCalendarParameters(HtmlCalendarConfiguration config)
    {
        Map<String, Object> params = getBasicParameters();

        //ToDo:Fix this using the configuration object!!!
        if(config.getProjectId() != null) {
            addSearchType("projectId", config.getProjectId(), params);
        } else {
            addSearchType("searchRequestId", config.getFilterId(), params);
        }

        params.put("config", config);

        ApplicationUser user = authenticationContext.getUser();
        try {
            CalendarSearchRequest calSearch = new CalendarSearchRequest(
                    user, config.getFilterId(), config.getProjectId(), config.getDateFieldName(),
                    config.getMonthStart().getTime(),
                    ParameterUtils.endOfMonth(config.getMonthStart().getTime()),
                    versionDelegator, permissionManager, projectManager, searchRequestService, searchService, customFieldManager);

            params.put("calSearch", calSearch);

            CalendarFactory calFactory = new CalendarFactory(calSearch, config.getDateFieldName());
            MonthEvents month = calFactory.createMonth(config.getMonthStart());
            params.put("month", month);

            params.put("previousMonthStr", ParameterUtils.getMonthStr(month.getPreviousMonthDate()));
            params.put("nextMonthStr", ParameterUtils.getMonthStr(month.getNextMonthDate()));
            params.put("displayMonthStr", ParameterUtils.getMonthStr(month.getMonthStartDate()));

        }
        catch(InvalidFilterSearchRequestException ise)
        {
            registerError(params, "portlet.issuecalendar.error.invalidFilter",
                        "The project chosen ("  + config.getProjectId() + ") doesn't exist or that user doesn't the appropriate permission. " + ise.getMessage(), ise);
        }
        catch(InvalidProjectSearchRequestException ipe)
        {
            registerError(params, "portlet.issuecalendar.error.invalidProject",
                         "The filter chosen ("  + config.getFilterId() + ") doesn't exist or that user doesn't the appropriate permission. " + ipe.getMessage(), ipe);
        }
        catch(SearchException e)
        {
            registerError(params, "portlet.issuecalendar.error.searchError",
                    "An error occured while the user's search was being executed. " + e.getMessage(), e);

        }

        return params;
    }

    private void registerError(Map<String, Object> startingParams, String errorKey, String logMessage, Exception e) {
        startingParams.put("errorKey", errorKey);
        log.error(logMessage, e);

    }

    private void addSearchType(String searchType, Long id, Map<String, Object> params)
    {
        params.put("searchType", searchType);
        params.put("id", id);
    }
}