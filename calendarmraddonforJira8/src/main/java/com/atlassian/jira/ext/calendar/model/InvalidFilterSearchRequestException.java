package com.atlassian.jira.ext.calendar.model;

public class InvalidFilterSearchRequestException extends Exception
{
    public InvalidFilterSearchRequestException(String reason)
    {
        super(reason);
    }
}
