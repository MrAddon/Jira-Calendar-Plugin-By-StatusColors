package com.atlassian.jira.ext.calendar.backdoor;

import com.atlassian.jira.testkit.client.BackdoorControl;
import com.atlassian.jira.testkit.client.JIRAEnvironmentData;
import com.sun.jersey.api.client.WebResource;

/**
 * Control for accessing the Calendar plugin servlet directly.
 */
public final class CalendarControl extends BackdoorControl<CalendarControl>
{
    public CalendarControl(JIRAEnvironmentData environmentData)
    {
        super(environmentData);
    }

    @Override
    protected WebResource createResource()
    {
        return resourceRoot(rootPath).path("plugins").
                path("servlet").
                path("calendar");
    }

    public String getCalendarForProjectAndField(final Long projectId, final String field)
    {
        final WebResource calendarServletResource = createResource().
                queryParam("projectId", Long.toString(projectId)).
                queryParam("dateFieldName", field);
        return calendarServletResource.get(String.class);
    }
}
