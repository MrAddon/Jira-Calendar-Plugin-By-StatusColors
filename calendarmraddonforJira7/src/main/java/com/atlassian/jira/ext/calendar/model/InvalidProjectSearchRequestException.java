package com.atlassian.jira.ext.calendar.model;

public class InvalidProjectSearchRequestException extends Exception
{
    public InvalidProjectSearchRequestException(String reason)
    {
        super(reason);
    }
}
