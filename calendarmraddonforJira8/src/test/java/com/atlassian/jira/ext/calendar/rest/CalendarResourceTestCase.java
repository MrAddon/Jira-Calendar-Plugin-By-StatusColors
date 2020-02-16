package com.atlassian.jira.ext.calendar.rest;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.ext.calendar.HtmlCalendar;
import com.atlassian.jira.ext.calendar.HtmlCalendarConfiguration;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.rest.v1.model.errors.ErrorCollection;
import com.atlassian.jira.rest.v1.model.errors.ValidationError;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.velocity.VelocityManager;
import junit.framework.Assert;
import org.apache.velocity.exception.VelocityException;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public class CalendarResourceTestCase extends MockObjectTestCase
{

    private Mock mockJiraAuthenticationContext;

    private Mock mockOfBizDelegator;

    private Mock mockVersionManager;

    private Mock mockPermissionManager;

    private Mock mockConstantsManager;

    private Mock mockVelocityManager;

    private Mock mockProjectManager;

    private Mock mockSearchRequestService;

    private Mock mockSearchService;

    private Mock mockApplicationProperties;

    private Mock mockCustomFieldManager;

    private CalendarResource calendarResource;

    private Mock mockI18nHelper;

    private Mock mockHttpServletRequest;

    private Mock mockHttpSession;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        mockJiraAuthenticationContext = new Mock(JiraAuthenticationContext.class);
        mockOfBizDelegator = new Mock(OfBizDelegator.class);
        mockVersionManager = new Mock(VersionManager.class);
        mockPermissionManager = new Mock(PermissionManager.class);
        mockConstantsManager = new Mock(ConstantsManager.class);
        mockVelocityManager = new Mock(VelocityManager.class);
        mockProjectManager = new Mock(ProjectManager.class);
        mockSearchRequestService = new Mock(SearchRequestService.class);
        mockSearchService = new Mock(SearchServiceBridge.class);
        mockApplicationProperties = new Mock(ApplicationProperties.class);
        mockCustomFieldManager = new Mock(CustomFieldManager.class);

        calendarResource = new TestCalendarResource();

        mockI18nHelper = new Mock(I18nHelper.class);
        mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpSession = new Mock(HttpSession.class);
    }

    public void testBlankProjectOrFilterIdFailsValidation()
    {
        Response r = calendarResource.validate("", "10");
        ErrorCollection errorCollection = (ErrorCollection) r.getEntity();

        Collection<ValidationError> validationErrors = errorCollection.getErrors();
        assertEquals(1, validationErrors.size());
        assertEquals(new ValidationError("projectOrFilterId", "gadget.common.required.query"), validationErrors.iterator().next());
    }

    public void testBlankNumOfIssueIconsFailsValidation()
    {
        Response r = calendarResource.validate("project-10000", "");
        ErrorCollection errorCollection = (ErrorCollection) r.getEntity();

        Collection<ValidationError> validationErrors = errorCollection.getErrors();
        assertEquals(1, validationErrors.size());
        assertEquals(new ValidationError("numOfIssueIcons", "gadget.issuescalendar.error.invalidNumberOfissues"), validationErrors.iterator().next());
    }

    public void testInvalidNumOfIssueIconsFailsValidation()
    {
        Response r = calendarResource.validate("project-10000", "invalid");
        ErrorCollection errorCollection = (ErrorCollection) r.getEntity();

        Collection<ValidationError> validationErrors = errorCollection.getErrors();
        assertEquals(1, validationErrors.size());
        assertEquals(new ValidationError("numOfIssueIcons", "gadget.issuescalendar.error.invalidNumberOfissues"), validationErrors.iterator().next());
    }

    public void testLesserThanZeroNumOfIssueIconsFailsValidation()
    {
        Response r = calendarResource.validate("project-10000", "-1");
        ErrorCollection errorCollection = (ErrorCollection) r.getEntity();

        Collection<ValidationError> validationErrors = errorCollection.getErrors();
        assertEquals(1, validationErrors.size());
        assertEquals(new ValidationError("numOfIssueIcons", "gadget.issuescalendar.error.invalidNumberOfissues"), validationErrors.iterator().next());
    }

    public void testOkResponseReturnedWhenValidationPasses()
    {
        Response r = calendarResource.validate("project-10000", "10");
        assertEquals(HttpServletResponse.SC_OK, r.getStatus());
    }

    public void testGetCalendarReturnsHtmlWithCalendarContainerDiv() throws VelocityException
    {
        mockHttpSession.expects(atLeastOnce()).method("getAttribute").with(ANYTHING).will(returnValue(null));
        mockHttpSession.expects(atLeastOnce()).method("setAttribute").with(ANYTHING, ANYTHING);
        mockHttpServletRequest.expects(atLeastOnce()).method("getSession").withNoArguments().will(returnValue(mockHttpSession.proxy()));

        mockApplicationProperties.expects(atLeastOnce()).method("getEncoding").withNoArguments().will(returnValue("UTF-8"));

        calendarResource = new TestCalendarResource()
        {
            @Override
            HtmlCalendar createHtmlCalendar()
            {
                return new HtmlCalendar(null, null, null, null, null, null, null, null)
                {
                    @Override
                    public Map<String, Object> getParameters(HtmlCalendarConfiguration config)
                    {
                        return Collections.emptyMap();
                    }
                };
            }
        };

        mockVelocityManager.expects(atLeastOnce()).method("getEncodedBody").with(
                eq("templates/plugins/jira/portlets/calendar/issuescalendar.vm"),
                ANYTHING,
                ANYTHING,
                ANYTHING
        ).will(returnValue("theHtml"));

        Response r = calendarResource.getCalendar(
                null, 10000L, null, null, "duedate", true, 10L, "projectTab", true, (HttpServletRequest) mockHttpServletRequest.proxy()
        );
        assertEquals("theHtml", r.getEntity());
    }

    public void testGetCalendarReturnsHtmlWithoutCalendarContainerDiv() throws VelocityException
    {
        mockHttpSession.expects(atLeastOnce()).method("getAttribute").with(ANYTHING).will(returnValue(null));
        mockHttpSession.expects(atLeastOnce()).method("setAttribute").with(ANYTHING, ANYTHING);
        mockHttpServletRequest.expects(atLeastOnce()).method("getSession").withNoArguments().will(returnValue(mockHttpSession.proxy()));

        mockApplicationProperties.expects(atLeastOnce()).method("getEncoding").withNoArguments().will(returnValue("UTF-8"));

        calendarResource = new TestCalendarResource()
        {
            @Override
            HtmlCalendar createHtmlCalendar()
            {
                return new HtmlCalendar(null, null, null, null, null, null, null, null)
                {
                    @Override
                    public Map<String, Object> getParameters(HtmlCalendarConfiguration config)
                    {
                        return Collections.emptyMap();
                    }
                };
            }
        };

        mockVelocityManager.expects(atLeastOnce()).method("getEncodedBody").with(
                eq("templates/plugins/jira/portlets/calendar/calendar.vm"),
                ANYTHING,
                ANYTHING,
                ANYTHING
        ).will(returnValue("theHtml"));

        Response r = calendarResource.getCalendar(
                null, 10000L, null, null, "duedate", true, 10L, "projectTab", false, (HttpServletRequest) mockHttpServletRequest.proxy()
        );
        assertEquals("theHtml", r.getEntity());
    }

    public void testGetCalendarWithBlankDateFieldDefaultsToIssueDueDate() throws VelocityException
    {
        final StringBuilder calendarConfigurationCheck = new StringBuilder();

        mockHttpSession.expects(atLeastOnce()).method("getAttribute").with(ANYTHING).will(returnValue(null));
        mockHttpSession.expects(atLeastOnce()).method("setAttribute").with(ANYTHING, ANYTHING);
        mockHttpServletRequest.expects(atLeastOnce()).method("getSession").withNoArguments().will(returnValue(mockHttpSession.proxy()));

        mockApplicationProperties.expects(atLeastOnce()).method("getEncoding").withNoArguments().will(returnValue("UTF-8"));

        calendarResource = new TestCalendarResource()
        {
            @Override
            HtmlCalendar createHtmlCalendar()
            {
                return new HtmlCalendar(null, null, null, null, null, null, null, null)
                {
                    @Override
                    public Map<String, Object> getParameters(HtmlCalendarConfiguration config)
                    {
                        calendarConfigurationCheck.append("true");
                        Assert.assertEquals(config.getDateFieldName(), DocumentConstants.ISSUE_DUEDATE);

                        return Collections.emptyMap();
                    }
                };
            }
        };

        mockVelocityManager.expects(atLeastOnce()).method("getEncodedBody").with(
                eq("templates/plugins/jira/portlets/calendar/issuescalendar.vm"),
                ANYTHING,
                ANYTHING,
                ANYTHING
        ).will(returnValue("theHtml"));

        Response r = calendarResource.getCalendar(
                null, 10000L, null, null, "", true, 10L, "projectTab", true, (HttpServletRequest) mockHttpServletRequest.proxy()
        );
        assertEquals("theHtml", r.getEntity());

        assertTrue(Boolean.valueOf(calendarConfigurationCheck.toString()));
    }

    public class TestCalendarResource extends CalendarResource
    {
        public TestCalendarResource()
        {
            super(
                    (JiraAuthenticationContext) mockJiraAuthenticationContext.proxy(),
                    (OfBizDelegator) mockOfBizDelegator.proxy(),
                    (VersionManager) mockVersionManager.proxy(),
                    (PermissionManager) mockPermissionManager.proxy(),
                    (ConstantsManager) mockConstantsManager.proxy(),
                    (VelocityManager) mockVelocityManager.proxy(),
                    (ProjectManager) mockProjectManager.proxy(),
                    (SearchRequestService) mockSearchRequestService.proxy(),
                    (SearchServiceBridge) mockSearchService.proxy(),
                    (ApplicationProperties) mockApplicationProperties.proxy(),
                    (CustomFieldManager) mockCustomFieldManager.proxy()
            );
        }
    }
}
