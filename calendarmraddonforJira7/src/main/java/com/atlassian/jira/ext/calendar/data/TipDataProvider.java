package com.atlassian.jira.ext.calendar.data;

import com.atlassian.jira.ext.calendar.SuppressedTipsManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserPropertyManager;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.json.marshal.Jsonable;
import com.atlassian.webresource.api.data.WebResourceDataProvider;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.Writer;

public class TipDataProvider implements WebResourceDataProvider
{
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final SuppressedTipsManager suppressedTipsManager;

    public TipDataProvider(JiraAuthenticationContext jiraAuthenticationContext, UserPropertyManager userPropertyManager)
    {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.suppressedTipsManager = new SuppressedTipsManager(userPropertyManager);
    }

    @Override
    public Jsonable get()
    {
        return new Jsonable()
        {
            @Override
            public void write(Writer writer) throws IOException
            {
                try
                {
                    new JSONObject(ImmutableMap.<String, Object>of("suppressTip", suppressTip())).write(writer);
                }
                catch (JSONException e)
                {
                    throw new JsonMappingException(e);
                }
            }
        };
    }

    private boolean suppressTip()
    {
        final ApplicationUser user = jiraAuthenticationContext.getUser();
        // We can't store preferences for anonymous users, so we never show them dismissible tips
        if (user == null)
        {
            return false;
        }

        try
        {
            return suppressedTipsManager.isSuppressed("browseProjectCalendarTab", user);
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }
}
