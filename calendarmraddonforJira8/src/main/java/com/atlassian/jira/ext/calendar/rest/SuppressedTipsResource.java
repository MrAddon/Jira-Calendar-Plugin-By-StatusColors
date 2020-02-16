package com.atlassian.jira.ext.calendar.rest;

import com.atlassian.jira.ext.calendar.SuppressedTipsManager;
import com.atlassian.jira.rest.api.http.CacheControl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * A REST resource to manage suppressed tips on a per-user basis.
 *
 * Note: this class is copied from jira-issue-nav-plugin. We should really move it into jira-api.
 */
@Path ("suppressedTips")
public class SuppressedTipsResource
{
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final SuppressedTipsManager suppressedTipsManager;

    public SuppressedTipsResource(JiraAuthenticationContext jiraAuthenticationContext, UserPropertyManager userPropertyManager)
    {
        this.jiraAuthenticationContext = jiraAuthenticationContext;

        // TODO: This really should be injected, but conditions are created in a DI container that doesn't contain
        // plugin components. The underlying cause of this problem is fixed in 5.1, so we can remove this hack then.
        this.suppressedTipsManager = new SuppressedTipsManager(userPropertyManager);
    }

    /**
     * Suppress a tip for the authenticated user.
     *
     * @param tipKey The key of the tip that is to be suppressed.
     * @return {@code 200 OK} if the request executed successfully, {@code 400 Bad Request} if {@code key} is not valid
     * tip key.
     */
    @POST
    @RequiresXsrfCheck
    public Response add(@FormParam ("tipKey") String tipKey)
    {
        try
        {
            ApplicationUser user = jiraAuthenticationContext.getUser();
            suppressedTipsManager.setSuppressed(tipKey, user, true);
            return Response.ok().cacheControl(CacheControl.never()).build();
        }
        catch (IllegalArgumentException e)
        {
            return Response.status(Response.Status.BAD_REQUEST).cacheControl(CacheControl.never()).build();
        }
    }
}