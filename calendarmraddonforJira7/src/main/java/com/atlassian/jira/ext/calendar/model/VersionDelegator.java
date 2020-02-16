package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.ofbiz.OfBizListIterator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import org.ofbiz.core.entity.EntityExpr;
import org.ofbiz.core.entity.EntityExprList;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericValue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Used to find lists of versions meeting certain conditions
 */
public class VersionDelegator
{
    private final OfBizDelegator ofBizDelegator;

    private final VersionManager versionManager;

    public VersionDelegator(OfBizDelegator ofbizDelegator, VersionManager versionManager)
    {
        this.ofBizDelegator = ofbizDelegator;
        this.versionManager = versionManager;
    }

    /**
     *
     * @param startDate
     * @param endDate
     * @param projects a set of {@link com.atlassian.jira.project.Project} objects representing the projects to be used in the search.
     *      List can be null or empty in which case only the time range will be used to restrict the
     *      search results.
     * @return a List of Version objects that are due for release in the given date range
     *      and associated with one of the projects in the set of projects provided.
     */
    public List getVersionsList(Date startDate, Date endDate, Set<Project> projects)
    {
        List<Version> versions = new ArrayList<Version>();
        if((projects != null) && (!projects.isEmpty()))
        {
            OfBizListIterator listIterator = null;
            try
            {
                EntityExpr condition = generateVersionSearchCondition(startDate, endDate, projects);
                listIterator = ofBizDelegator.findListIteratorByCondition("Version", condition);

                GenericValue gv;
                while (null != (gv = listIterator.next()))
                {
                    Long versionId = gv.getLong("id");
                    versions.add(versionManager.getVersion(versionId));
                }
            }
            finally
            {
                if (listIterator != null)
                {
                    listIterator.close();
                }
            }
        }
        return versions;
    }

    private static EntityExpr generateVersionSearchCondition(Date startDate, Date endDate, Set<Project> projects)
    {
        Timestamp t1 = new Timestamp(startDate.getTime());
        Timestamp t2 = new Timestamp(endDate.getTime());
        EntityExpr r1 = new EntityExpr("releasedate", EntityOperator.GREATER_THAN_EQUAL_TO, t1);
        EntityExpr r2 = new EntityExpr("releasedate", EntityOperator.LESS_THAN_EQUAL_TO, t2);
        EntityExpr releaseDateCondition = new EntityExpr(r1, EntityOperator.AND, r2);
        List<EntityExpr> projectIdsConditionList = new ArrayList<EntityExpr>();

        for(Project project : projects)
            projectIdsConditionList.add(new EntityExpr("project", EntityOperator.EQUALS, project.getId()));

        EntityExprList projectIdsCondition = new EntityExprList(projectIdsConditionList, EntityOperator.OR);

        return new EntityExpr(releaseDateCondition, EntityOperator.AND, projectIdsCondition);
    }

}
