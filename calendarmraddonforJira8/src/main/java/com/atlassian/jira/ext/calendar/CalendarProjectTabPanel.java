package com.atlassian.jira.ext.calendar;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.ext.calendar.model.VersionDelegator;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.plugin.projectpanel.ProjectTabPanelModuleDescriptor;
import com.atlassian.jira.plugin.projectpanel.impl.GenericProjectTabPanel;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.browse.BrowseContext;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.web.FieldVisibilityManager;
import webwork.action.ServletActionContext;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;

public class CalendarProjectTabPanel extends GenericProjectTabPanel
{
    private final OfBizDelegator ofBizDelegator;

    private final VersionManager versionManager;

    private final PermissionManager permissionManager;

    private final ConstantsManager constantsManager;

    private final ProjectManager projectManager;

    private final SearchRequestService searchRequestService;

    private final SearchServiceBridge searchService;

    private final CustomFieldManager customFieldManager;

    private final FieldVisibilityManager fieldVisibilityManager;

    public CalendarProjectTabPanel(JiraAuthenticationContext jiraAuthenticationContext, OfBizDelegator ofBizDelegator, VersionManager versionManager, PermissionManager permissionManager, ConstantsManager constantsManager, ProjectManager projectManager, SearchRequestService searchRequestService, SearchServiceBridge searchService, CustomFieldManager customFieldManager, FieldVisibilityManager fieldVisibilityManager)
    {
        super(jiraAuthenticationContext);
        this.ofBizDelegator = ofBizDelegator;
        this.versionManager = versionManager;
        this.permissionManager = permissionManager;
        this.constantsManager = constantsManager;
        this.projectManager = projectManager;
        this.searchRequestService = searchRequestService;
        this.searchService = searchService;
        this.customFieldManager = customFieldManager;
        this.fieldVisibilityManager = fieldVisibilityManager;
    }

    @Override
    public void init(ProjectTabPanelModuleDescriptor projectTabPanelModuleDescriptor)
    {
        super.init(projectTabPanelModuleDescriptor);
    }

    public String getHtml(BrowseContext browseContext)
    {
        final Map<String, Object> startingParams = new HashMap<String, Object>();
        final HtmlCalendar htmlCalendar = new HtmlCalendar(authenticationContext, new VersionDelegator(ofBizDelegator, versionManager), permissionManager, constantsManager, projectManager, searchRequestService, searchService, customFieldManager);
        final HttpSession sess = ServletActionContext.getRequest().getSession(false);

        startingParams.put("fieldVisibility", fieldVisibilityManager);
        startingParams.put("portlet", this);
        startingParams.putAll(htmlCalendar.getParameters(browseContext, sess));

        return descriptor.getHtml("view", startingParams);
    }
}
