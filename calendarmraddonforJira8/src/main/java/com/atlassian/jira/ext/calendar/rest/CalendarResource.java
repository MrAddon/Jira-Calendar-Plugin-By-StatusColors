package com.atlassian.jira.ext.calendar.rest;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.ext.calendar.HtmlCalendar;
import com.atlassian.jira.ext.calendar.HtmlCalendarConfiguration;
import com.atlassian.jira.ext.calendar.ParameterUtils;
import com.atlassian.jira.ext.calendar.model.GregorianCalendarFactory;
import com.atlassian.jira.ext.calendar.model.VersionDelegator;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.rest.v1.model.errors.ErrorCollection;
import com.atlassian.jira.rest.v1.model.errors.ValidationError;
import com.atlassian.jira.rest.v1.util.CacheControl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.velocity.VelocityManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Path("calendar")
@AnonymousAllowed
public class CalendarResource
{
    private static final Logger LOG = Logger.getLogger(CalendarResource.class);

    private final JiraAuthenticationContext jiraAuthenticationContext;

    private final OfBizDelegator ofBizDelegator;

    private final VersionManager versionManager;

    private final PermissionManager permissionManager;

    private final ConstantsManager constantsManager;

    private final VelocityManager velocityManager;

    private final ProjectManager projectManager;

    private final SearchRequestService searchRequestService;

    private final SearchServiceBridge searchService;

    private final ApplicationProperties applicationProperties;

    private final CustomFieldManager customFieldManager;

    public CalendarResource(JiraAuthenticationContext jiraAuthenticationContext, OfBizDelegator ofBizDelegator, VersionManager versionManager, PermissionManager permissionManager, ConstantsManager constantsManager, VelocityManager velocityManager, ProjectManager projectManager, SearchRequestService searchRequestService, SearchServiceBridge searchService, ApplicationProperties applicationProperties, CustomFieldManager customFieldManager)
    {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.ofBizDelegator = ofBizDelegator;
        this.versionManager = versionManager;
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.velocityManager = velocityManager;
        this.projectManager = projectManager;
        this.searchRequestService = searchRequestService;
        this.searchService = searchService;
        this.applicationProperties = applicationProperties;
        this.customFieldManager = customFieldManager;
    }

    @GET
    @Path("htmlcalendar/config/datefields")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDateFields()
    {
        List<DateField> dateFields = new ArrayList<DateField>();
        I18nHelper i18nHelper = jiraAuthenticationContext.getI18nHelper();

        dateFields.add(createDateField(DocumentConstants.ISSUE_DUEDATE, i18nHelper.getText("issue.field.duedate")));
        dateFields.add(createDateField(DocumentConstants.ISSUE_CREATED, i18nHelper.getText("issue.field.created")));
        dateFields.add(createDateField(DocumentConstants.ISSUE_UPDATED, i18nHelper.getText("issue.field.updated")));

        for(CustomField customField : customFieldManager.getCustomFieldObjects())
        {
            CustomFieldType type = customField.getCustomFieldType();
            if((type instanceof DateCFType) || (type instanceof DateTimeCFType))
            {
            	List<Project> associatedProjects = customField.getAssociatedProjectObjects();
            	if (associatedProjects == null || associatedProjects.isEmpty() || hasPermission(customField))
            	{
            		dateFields.add(createDateField(customField.getId(), customField.getName()));
            	}
            }
        }

        DateFields _dateFields = new DateFields();
        _dateFields.dateField = dateFields;

        return Response.ok(_dateFields).cacheControl(CacheControl.NO_CACHE).build();
    }
    
    private boolean hasPermission(CustomField customField)
    {
    	List<Project> projects = customField.getAssociatedProjectObjects();
    	for(Project project : projects)
    	{
	    	if(permissionManager.hasPermission(Permissions.BROWSE, project, jiraAuthenticationContext.getLoggedInUser()))
	    	    return true;
    	}
    	
		return false;
    }

    private DateField createDateField(String value, String label)
    {
        DateField dateField = new DateField();
        dateField.value = value;
        dateField.label = label;
        return dateField;
    }

    @XmlRootElement
    public static class DateField
    {
        @XmlElement
        public String label;

        @XmlElement
        public String value;
    }

    @XmlRootElement
    public static class DateFields
    {
        @XmlElement
        public List<DateField> dateField;
    }

    @GET
    @Path("htmlcalendar")
    @Produces(MediaType.TEXT_HTML)
    public Response getCalendar(
            @QueryParam("searchRequestId") Long searchRequestId,
            @QueryParam("projectId") Long projectId,
            @QueryParam("month") String month,
            @QueryParam("portletId") String portletIdString,
            @QueryParam("dateFieldName") String dateFieldName,
            @QueryParam("displayVersions") Boolean displayVersions,
            @QueryParam("numOfIssueIcons") Long numOfIssueIcons,
            @QueryParam("context") String context,
            @QueryParam("includeContainer") @DefaultValue("false") boolean includeContainer,
            @Context HttpServletRequest httpServletRequest) throws VelocityException
    {
        HtmlCalendar htmlCalendar = createHtmlCalendar();

        Calendar monthStart = getChosenMonth(month);
        HttpSession httpSession = httpServletRequest.getSession();
        Long portletId = getPortletId(portletIdString);
        Long key = HtmlCalendarConfiguration.PROJECT_TAB_CONTEXT.equals(context) ? projectId : portletId;

        if (null == monthStart)
            monthStart = HtmlCalendarConfiguration.getBrowseMonth(httpSession, key, context );
        
        HtmlCalendarConfiguration configuration = new HtmlCalendarConfiguration(
                searchRequestId,
                projectId,
                monthStart,
                portletId,
                StringUtils.isBlank(dateFieldName) ? DocumentConstants.ISSUE_DUEDATE : dateFieldName,
                displayVersions,
                numOfIssueIcons,
                context
        );

        HtmlCalendarConfiguration.setDisplayVersions(httpSession, key, displayVersions, context);
        HtmlCalendarConfiguration.setBrowseMonth(httpSession, key, monthStart, context);

        Map<String, Object> velocityParams = htmlCalendar.getParameters(configuration);

        return Response.ok(
                velocityManager.getEncodedBody(
                        includeContainer ? "templates/plugins/jira/portlets/calendar/issuescalendar.vm" : "templates/plugins/jira/portlets/calendar/calendar.vm",
                        "",
                        applicationProperties.getEncoding(),
                        velocityParams
                ))
                .cacheControl(CacheControl.NO_CACHE).build();
    }

    private Long getPortletId(String portletIdString)
    {
        try
        {
            return StringUtils.isBlank(portletIdString) ? null : Long.parseLong(portletIdString);
        }
        catch (NumberFormatException nfe)
        {
            LOG.warn("Unable to convert portlet/gadget ID " + portletIdString + " to a number.", nfe);
            return (long) portletIdString.hashCode();
        }
    }

    ///CLOVER:OFF
    HtmlCalendar createHtmlCalendar()
    {
        return new HtmlCalendar(jiraAuthenticationContext, new VersionDelegator(ofBizDelegator, versionManager), permissionManager, constantsManager, projectManager, searchRequestService, searchService, customFieldManager);
    }
    ///CLOVER:ON

    private static Calendar getChosenMonth(String month)
    {
        try {
            Date monthStartDate = ParameterUtils.getMonth(month);
            return GregorianCalendarFactory.constructCalendar(monthStartDate);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @GET
    @Path("htmlcalendar/config/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validate(
            @QueryParam("projectOrFilterId") String projectOrFilterId,
            @QueryParam("numOfIssueIcons") String numOfIssueIcons)
    {
        Collection<ValidationError> validationErrors = new ArrayList<ValidationError>();

        if (StringUtils.isBlank(projectOrFilterId))
            validationErrors.add(new ValidationError("projectOrFilterId", "gadget.common.required.query"));

        try
        {
            Long numOfIssueIconsLong = new Long(StringUtils.defaultString(numOfIssueIcons));
            if (numOfIssueIconsLong < 0)
                validationErrors.add(new ValidationError("numOfIssueIcons", "gadget.issuescalendar.error.invalidNumberOfissues"));
        }
        catch (NumberFormatException nfe)
        {
            validationErrors.add(new ValidationError("numOfIssueIcons", "gadget.issuescalendar.error.invalidNumberOfissues"));
        }

        if (validationErrors.isEmpty())
        {
            return Response.ok().cacheControl(CacheControl.NO_CACHE).build();
        }
        else
        {
            return Response.status(HttpServletResponse.SC_BAD_REQUEST)
                    .entity(ErrorCollection.Builder.newBuilder(validationErrors).build())
                    .cacheControl(CacheControl.NO_CACHE)
                    .build();
        }
    }
}
