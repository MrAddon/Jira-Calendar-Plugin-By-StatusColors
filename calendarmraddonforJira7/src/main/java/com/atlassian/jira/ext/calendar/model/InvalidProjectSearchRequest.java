package com.atlassian.jira.ext.calendar.model;

public class InvalidProjectSearchRequest extends Exception
{
    public InvalidProjectSearchRequest(String reason)
    {
        super(reason);
    }
}
