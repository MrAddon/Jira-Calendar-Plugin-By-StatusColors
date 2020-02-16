package com.atlassian.jira.ext.calendar.model;

import com.atlassian.core.util.DateUtils;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlOrderByBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.context.QueryContext;
import com.atlassian.jira.jql.context.QueryContext.ProjectIssueTypeContexts;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.QueryImpl;
import com.atlassian.query.clause.AndClause;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.operator.Operator;
import com.atlassian.query.order.SortOrder;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A search request to produce a calendar
 */
public class CalendarSearchRequest
{
    /**
     * The default start offset for the search request in milliseconds, currently 4 weeks.
     */
    private static final Long DEFAULT_START_OFFSET = -4 * 7 * DateUtils.DAY_MILLIS;

    /**
     * The default end offset for the search request in milliseconds, currently 4 weeks.
     */
    private static final Long DEFAULT_END_OFFSET = 12 * 7 * DateUtils.DAY_MILLIS;

    private final ApplicationUser user;

    private final SearchRequest projectOrFilterIdSearchRequest;

    private final SearchRequest search;

    private final DateFieldProvider dateFieldProvider;

    private final Date startDate;

    private final Date endDate;

    private final PermissionManager permissionManager;

    private final ProjectManager projectManager;

    private final CustomFieldManager customFieldManager;

    private final SearchRequestService searchRequestService;

    private final SearchServiceBridge searchService;

    private SortedSet<Issue> issues;

    private SortedSet<Version> versions;

    private Long projectId;

    /**
     * Creates a search request relative to the current time
     *
     * @param user              The current authenticated user.
     * @param searchIdentifier  The ID of a search filter. This can be null if <tt>projectId</tt> is not.
     * @param projectId         Can be null.
     *                          The ID of a project. This can be null if <tt>searchIdentifier</tt> is not.
     * @param dateFieldName     The name of the date field that will be used as part of the search query criteria.
     * @param startOffset       How far back should issues be returned based on the current, in milliseconds.
     * @param endOffset         in milliseconds
     *                          How far forward should issues be returned based on the current, in milliseconds.
     * @param versionDelegator  See {@link com.atlassian.jira.ext.calendar.model.VersionDelegator}
     * @param permissionManager See {@link com.atlassian.jira.security.PermissionManager}
     * @param projectManager See {@link com.atlassian.jira.project.ProjectManager}
     * @param searchRequestService See {@link com.atlassian.jira.bc.filter.SearchRequestService}
     * @throws InvalidFilterSearchRequestException
     *                         Thrown if <tt>searchIdentifier</tt> is not <tt>null</tt> but invalid.
     * @throws InvalidProjectSearchRequestException
     *                         Thrown if <tt>projectId</tt> is not <tt>null</tt> but invalid.
     * @throws SearchException Thrown if there is a problem searching for issues based on the specified parameters.
     */
    public CalendarSearchRequest(ApplicationUser user, Long searchIdentifier, Long projectId, String dateFieldName, Long startOffset, Long endOffset,
            VersionDelegator versionDelegator, PermissionManager permissionManager, ProjectManager projectManager, SearchRequestService searchRequestService, SearchServiceBridge searchService, CustomFieldManager customFieldManager)
            throws InvalidFilterSearchRequestException, InvalidProjectSearchRequestException, SearchException
    {
        this(
                user,
                searchIdentifier,
                projectId,
                dateFieldName,
                new Date(System.currentTimeMillis() + (null == startOffset ? DEFAULT_START_OFFSET : startOffset)),
                new Date(System.currentTimeMillis() + (null == endOffset ? DEFAULT_END_OFFSET : endOffset)),
                versionDelegator,
                permissionManager,
                projectManager,
                searchRequestService,
                searchService,
                customFieldManager);
    }

    /**
     * Pretty much like {@link #CalendarSearchRequest(com.atlassian.jira.user.ApplicationUser, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Long, java.lang.Long, com.atlassian.jira.ext.calendar.model.VersionDelegator, com.atlassian.jira.security.PermissionManager, com.atlassian.jira.project.ProjectManager, com.atlassian.jira.bc.filter.SearchRequestService, com.atlassian.jira.compatibility.bridge.search.SearchServiceBridge, com.atlassian.jira.issue.CustomFieldManager)}
     * but with absolute date ranges.
     */
    public CalendarSearchRequest(ApplicationUser user, Long searchIdentifier, Long projectId, String dateFieldName, Date startDate, Date endDate,
            VersionDelegator versionDelegator, PermissionManager permissionManager, ProjectManager projectManager, SearchRequestService searchRequestService, SearchServiceBridge searchService, CustomFieldManager customFieldManager)
            throws InvalidFilterSearchRequestException, InvalidProjectSearchRequestException, SearchException
    {
        final long now = System.currentTimeMillis();

        this.user = user;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.searchRequestService = searchRequestService;
        this.searchService = searchService;
        this.dateFieldProvider = new DateFieldProvider(dateFieldName);
        this.startDate = null == startDate ? new Date(now + DEFAULT_START_OFFSET) : startDate;
        this.endDate = null == endDate ? new Date(now + DEFAULT_END_OFFSET) : endDate;
        this.issues = new TreeSet<Issue>(new IssueComparator(dateFieldName));
        this.versions = new TreeSet<Version>(new VersionComparator());
        this.projectOrFilterIdSearchRequest = getSearchForProjectOrSearchId(searchIdentifier, projectId, user);
        this.search = new SearchRequest(projectOrFilterIdSearchRequest.getQuery());
        this.projectId = projectId;
        this.customFieldManager = customFieldManager;

        this.search.setName(projectOrFilterIdSearchRequest.getName());

        addConstraintToSearchRequest(this.search, dateFieldName, this.startDate, this.endDate);
        orderByDateFieldAndPriority(this.search, dateFieldName);

        initializeIssues();
        initializeVersions(versionDelegator);
    }

    /**
     * Returns either a search filter or a search request constrained to a specific project.
     *
     * @param searchIdentifier The ID of a search filter. This can be null if <tt>projectId</tt> is not.
     * @param projectId        Can be null.
     *                         The ID of a project. This can be null if <tt>searchIdentifier</tt> is not.
     * @param user             The current authenticated user.
     * @return A new search request depending on the parameters.
     * @throws InvalidFilterSearchRequestException
     *                         Thrown if <tt>searchIdentifier</tt> is not <tt>null</tt> but invalid.
     * @throws InvalidProjectSearchRequestException
     *                         Thrown if <tt>projectId</tt> is not <tt>null</tt> but invalid.
     */
    protected SearchRequest getSearchForProjectOrSearchId(Long searchIdentifier, Long projectId, ApplicationUser user)
            throws InvalidFilterSearchRequestException, InvalidProjectSearchRequestException
    {
        if (searchIdentifier != null)
        {
            final SearchRequest tempSearch = searchRequestService.getFilter(new JiraServiceContextImpl(user), searchIdentifier);

            if (tempSearch == null)
            {
                throw new InvalidFilterSearchRequestException("Invalid searchId given " + searchIdentifier);
            }
            SearchRequest retSearch = new SearchRequest(tempSearch);
            retSearch.setName(tempSearch.getName());
            return retSearch;
        }
        else if (projectId != null)
        {
            Project project = projectManager.getProjectObj(projectId);
            if (project == null)
            {
                throw new InvalidFilterSearchRequestException("Invalid projectId given " + projectId);
            }
            return createProjectSearchRequest(projectId, project.getName());
        }
        else
        {
            throw new InvalidProjectSearchRequestException("No SearchId or ProjectId specified.");
        }
    }

    private void initializeIssues()
            throws SearchException
    {
        SearchResults results = getResults();

        for (Issue issue : results.getIssues())
            if (isCalendarableIssue(issue))
                issues.add(issue);
    }

    @SuppressWarnings("unchecked")
    private void initializeVersions(VersionDelegator versionDelegator)
    {
        final Set<Project> allowedProjects = new HashSet<Project>(permissionManager.getProjects(ProjectPermissions.BROWSE_PROJECTS, user));
        Set<Project> projects = null;

        /*
         * JCAL-101 --- https://developer.atlassian.com/jira/browse/JCAL-101
         * Previously when projectId is null/empty, it'll just default to all projects because in JIRA 4.0,
         * you can no longer go through all the project parameters. Now we have found a way to figure out what
         * projects is included via JQL, just have JIRA to help us return the result from executing the query.
         * You don't need complex coding or manual detection to get the projects selected in JQL.
         */
        if (null == projectId)
        {
            if (StringUtils.isBlank(projectOrFilterIdSearchRequest.getQuery().getQueryString()) || !projectOrFilterIdSearchRequest.getQuery().getQueryString().contains("project"))
            {
                // No query string detected default to all projects
                projects = new HashSet<Project>(allowedProjects);
            }
            else
            {
                QueryContext queryContext = searchService.getSimpleQueryContext(user, projectOrFilterIdSearchRequest.getQuery());
                Collection queryProjects = queryContext.getProjectIssueTypeContexts();

                HashSet<Project> selectedProjects = new HashSet<Project>();
                if (null != queryProjects)
                {
                    for (Object contextObject: queryProjects)
                    {
                        selectedProjects.addAll(
                                Collections2.filter(
                                        Collections2.transform(
                                                ((ProjectIssueTypeContexts) contextObject).getProjectIdInList(),
                                                new Function<Long, Project>()
                                                {
                                                    public Project apply(Long projectId)
                                                    {
                                                        return projectManager.getProjectObj(projectId);
                                                    }
                                                }
                                        ),
                                        Predicates.and(Predicates.<Project>notNull(), new Predicate<Project>()
                                        {
                                            public boolean apply(Project p)
                                            {
                                                return allowedProjects.contains(p);
                                            }
                                        })
                                )
                        );
                    }
                }
                projects = new HashSet<Project>(selectedProjects);
            }
        }
        else
        {
            Project targetProject = projectManager.getProjectObj(projectId);
            if (isProjectInAllowedList(allowedProjects, targetProject))
                projects = new HashSet<Project>(Arrays.asList(targetProject));
        }

        versions.addAll(
                versionDelegator.getVersionsList(
                        startDate,
                        endDate,
                        projects
                )
        );
    }

    /**
     * This way of checking is required because some {@link com.atlassian.jira.project.ProjectImpl}s loaded by
     * {@link com.atlassian.jira.project.ProjectManager#getProjectObj(Long)} } returns an instance without the
     * project lead property field when there is one. When there's the same project in the allowed list and it is
     * populated with the lead property while <tt>targetProject</tt> is not, the check is done via
     * {@link java.util.Set#contains(Object)} }, you'll get a false negative.
     *
     * @param allowedProjects
     * The set of projects that {@link #user} can browse.
     * @param targetProject
     * The project to look for in the set above.
     * @return
     * <tt>true</tt> if <tt>targetProject</tt> is in the <tt>allowedProjects</tt> set; <tt>false</tt> otherwise.
     */
    private boolean isProjectInAllowedList(Set<Project> allowedProjects, Project targetProject)
    {
        for (Project allowedProject: allowedProjects)
            if (StringUtils.equalsIgnoreCase(allowedProject.getKey(), targetProject.getKey()))
                return true;

        return false;
    }

    private String getClauseNameOfCustomField(String customFieldIdString)
    {
        return new StringBuilder("cf[").append(customFieldIdString.substring("customfield_".length())).append("]").toString();
    }

    /**
     * Remove hours, minutes, seconds and ms entirely. See JCAL-176.
     */
    private Date truncate(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.setTimeZone(ComponentManager.getComponentInstanceOfType(TimeZoneManager.class).getLoggedInUserTimeZone());

        calendar.set(Calendar.HOUR_OF_DAY, 0); // start at midnight
        calendar.set(Calendar.MINUTE, 0); // start at midnight
        calendar.set(Calendar.SECOND, 0); // start at midnight
        calendar.set(Calendar.MILLISECOND, 0); // start at midnight
        Date truncatedDate = calendar.getTime();
        return truncatedDate;
    }

    /**
     * Constraints a search request to return results based on the date range specified.
     *
     * @param sr            The search request to constraint
     * @param dateFieldName The date field name &mdash; one of {@link com.atlassian.jira.issue.index.DocumentConstants#ISSUE_DUEDATE},
     *                      {@link com.atlassian.jira.issue.index.DocumentConstants#ISSUE_CREATED}, {@link com.atlassian.jira.issue.index.DocumentConstants#ISSUE_UPDATED},
     * @param start         The lower bound of the date range
     * @param end           The upper bound of the date range.
     */
    private void addConstraintToSearchRequest(SearchRequest sr, String dateFieldName, Date start, Date end)
    {
        Clause whereClause;
        JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder();

        if (StringUtils.equals(DocumentConstants.ISSUE_DUEDATE, dateFieldName))
        {
            // Need to truncate dates to the day - JIRA 4.4 does not accept dates with time for
            // date fields that do not denote time (like due date)
            // JCAL-176 - There is a bug in the apache truncate method that does not work properly
            //          - For the time being we have to manually truncate the time for the date fields
            //whereClause = jqlQueryBuilder.where().dueBetween(
            //        org.apache.commons.lang.time.DateUtils.truncate(start, Calendar.DAY_OF_MONTH),
            //        org.apache.commons.lang.time.DateUtils.truncate(end, Calendar.DAY_OF_MONTH)
            //).buildQuery().getWhereClause();

            whereClause = jqlQueryBuilder.where().dueBetween(truncate(start), truncate(end)).buildQuery().getWhereClause();
        }
        else if (StringUtils.equals(DocumentConstants.ISSUE_CREATED, dateFieldName))
            whereClause = jqlQueryBuilder.where().createdBetween(start, end).buildQuery().getWhereClause();
        else if (StringUtils.equals(DocumentConstants.ISSUE_UPDATED, dateFieldName))
            whereClause = jqlQueryBuilder.where().updatedBetween(start, end).buildQuery().getWhereClause();
        else
        {
        	String clauseName = getClauseNameOfCustomField(dateFieldName);

            // Need to truncate dates to the day - JIRA 4.4 does not accept dates with time for
            // date fields that do not denote time (like due date)
        	CustomField customField = customFieldManager.getCustomFieldObject(dateFieldName);
            CustomFieldType type = null == customField ? null : customField.getCustomFieldType();

            if(type instanceof DateCFType)
            {
                whereClause = jqlQueryBuilder.where()
                    .addDateCondition(
                            clauseName,
                            Operator.GREATER_THAN_EQUALS,
                            // JCAL-176 - There is a bug in the apache truncate method that does not work properly
                            //          - For the time being we have to manually truncate the time for the date fields
                            //org.apache.commons.lang.time.DateUtils.truncate(start, Calendar.DAY_OF_MONTH))
                            truncate(start))
                    .and()
                    .addDateCondition(
                            clauseName,
                            Operator.LESS_THAN_EQUALS,
                            // JCAL-176 - Read comments above
                            //org.apache.commons.lang.time.DateUtils.truncate(end, Calendar.DAY_OF_MONTH))
                            truncate(end))
                    .buildQuery().getWhereClause();
            }
            else
            {
            	whereClause = jqlQueryBuilder.where()
		                .addDateCondition(clauseName, Operator.GREATER_THAN_EQUALS, start)
		                .and()
		                .addDateCondition(clauseName, Operator.LESS_THAN_EQUALS, end)
		                .buildQuery().getWhereClause();
            }
        }

        if (null != whereClause)
        {
            Query searchQuery = sr.getQuery();
            if (null != searchQuery.getWhereClause())
                sr.setQuery(new QueryImpl(new AndClause(searchQuery.getWhereClause(), whereClause), searchQuery.getOrderByClause(), null));
            else
                sr.setQuery(jqlQueryBuilder.buildQuery());
        }
    }

    /**
     * Adds result order to a search request.
     *
     * @param sr            The search request to that will be used to query for the ordered result.
     * @param dateFieldName The date field name &mdash; one of {@link com.atlassian.jira.issue.index.DocumentConstants#ISSUE_DUEDATE},
     *                      {@link com.atlassian.jira.issue.index.DocumentConstants#ISSUE_CREATED}, {@link com.atlassian.jira.issue.index.DocumentConstants#ISSUE_UPDATED},
     */
    private void orderByDateFieldAndPriority(SearchRequest sr, String dateFieldName)
    {
        JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder(sr.getQuery());
        JqlOrderByBuilder jqlOrderByBuilder = jqlQueryBuilder.orderBy();

        if (StringUtils.equals(DocumentConstants.ISSUE_DUEDATE, dateFieldName))
            jqlOrderByBuilder = jqlOrderByBuilder.dueDate(SortOrder.DESC);
        else if (StringUtils.equals(DocumentConstants.ISSUE_CREATED, dateFieldName))
            jqlOrderByBuilder = jqlOrderByBuilder.createdDate(SortOrder.DESC);
        else if (StringUtils.equals(DocumentConstants.ISSUE_UPDATED, dateFieldName))
            jqlOrderByBuilder = jqlOrderByBuilder.updatedDate(SortOrder.DESC);
        else
            jqlOrderByBuilder.add(getClauseNameOfCustomField(dateFieldName), SortOrder.DESC);

        jqlOrderByBuilder.priority(SortOrder.ASC);

        sr.setQuery(jqlQueryBuilder.buildQuery());
    }

    /**
     * Creates a search request for a given project
     *
     * @param projectId   The project's numeric ID.
     * @param projectName The project's name, which will be used as the name of the search request created.
     * @return A {@link SearchRequest} for all issues related to a given project.
     */
    private SearchRequest createProjectSearchRequest(Long projectId, String projectName)
    {
        SearchRequest sr = new SearchRequest(JqlQueryBuilder.newBuilder().where().project(projectId).buildQuery());
        sr.setName(projectName);
        return sr;
    }

    public SearchRequest getSearch()
    {
        return search;
    }

    public DateFieldProvider getDateFieldProvider()
    {
        return dateFieldProvider;
    }

    public SearchResults getResults() throws SearchException
    {
        SearchProvider searcher = getSearchProvider();
        return searcher.search(search.getQuery(), user, PagerFilter.getUnlimitedFilter());
    }

    protected SearchProvider getSearchProvider()
    {
        return ComponentAccessor.getComponent(SearchProvider.class);
    }

    /**
     * Checks if an issue can be displayed on the calendar based on this search request.
     * @param issue
     * The issue to check
     * @return Returns <tt>true</tt> if the issue can be displayed; <tt>false</tt> otherwise.
     */
    public boolean isCalendarableIssue(Issue issue)
    {
        return (issue != null) && (dateFieldProvider.getDate(issue) != null);
    }

    /**
     * Checks if a version can be displayed on the calendar based on this search request.
     * @param version
     * The version to check
     * @return Returns <tt>true</tt> if the issue can be displayed; <tt>false</tt> otherwise.
     */
    public boolean isCalendarableVersion(Version version)
    {
        long nowInMillis = new Date().getTime();
        long startOffset = startDate.getTime() - nowInMillis;
        long endOffset = endDate.getTime() - nowInMillis;

        return (version != null) && (version.getReleaseDate() != null) &&
                (isDateInRange(version.getReleaseDate(), startOffset, endOffset));
    }

    private static boolean isDateInRange(Date date, long startOffset, long endOffset)
    {
        long now = System.currentTimeMillis();

        return ((date.getTime() >= (now + startOffset)) &&
                (date.getTime() <= (now + endOffset)));
    }

    public SortedSet getIssues()
    {
        return issues;
    }

    public SortedSet getVersions()
    {
        return versions;
    }

    /**
     * Returns a copy of the SearchRequest but only with the results for the specific day
     * @param date
     * The day
     * @return
     * A new {@link SearchRequest} based on the day specified.
     */
    public SearchRequest getSearchRequestForDay(Date date)
    {
        // truncate to day
        Date truncatedDate = date;
        if (truncatedDate != null)
        {
            truncatedDate = org.apache.commons.lang.time.DateUtils.truncate(date, Calendar.DAY_OF_MONTH);
        }

        // certain fields are treated as inclusive at period ends, others are exlusive
        String dateFieldName = dateFieldProvider.getDateFieldName();
        boolean exclusive = dateFieldName.equals(DocumentConstants.ISSUE_DUEDATE) ||
                (dateFieldName.startsWith(DocumentConstants.ISSUE_CUSTOMFIELD_PREFIX) && !dateFieldProvider.isDateTimeField());

        Date dayStartDate = truncatedDate;
        Date dayEndDate = exclusive ? truncatedDate : new Date(truncatedDate.getTime() + DateUtils.DAY_MILLIS);

        SearchRequest searchRequestForDay = new SearchRequest(projectOrFilterIdSearchRequest);
        addConstraintToSearchRequest(searchRequestForDay, dateFieldName, dayStartDate, dayEndDate);

        return searchRequestForDay;
    }

    public String getQueryString()
    {
        return projectOrFilterIdSearchRequest.isLoaded()
                ? new StringBuilder("?mode=hide&requestId=").append(projectOrFilterIdSearchRequest.getId()).toString()
                : new StringBuilder("?reset=true&").append(searchService.getQueryString(user, projectOrFilterIdSearchRequest.getQuery())).toString();
    }
}
