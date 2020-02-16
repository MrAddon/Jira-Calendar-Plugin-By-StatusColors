package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.util.I18nHelper;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import org.apache.log4j.Logger;

import java.sql.Timestamp;

/**
 * This helper extracts standard or custom date fields from {@link Issue} objects
 * by their names.
 */
public class DateFieldProvider
{
    private static final Logger log = Logger.getLogger(DateFieldProvider.class.getName());

	/**
	 * The field can either be a standard or custom field:
     * <ul>
     * <li>null: the default <i>dueDate</i></li>
     * <li>a standard field, like <i>dueDate</i> or <i>updated</i></li>
     * <li>otherwise if a string prefixed with <code>DocumentConstants.ISSUE_CUSTOMFIELD_PREFIX</code>: a custom field of date type</li>
     * </ul>
     */
	private String dateFieldName;

    public DateFieldProvider(String dateFieldName)
    {
		this.dateFieldName = dateFieldName;
	}

	public String getDateFieldName()
	{
		return dateFieldName;
	}

	/**
	 * Gets the date field name to display in the UI.
	 */
    public String getI18nDateFieldName()
    {
        final I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();

    	if(dateFieldName != null)
    	{
    		if(dateFieldName.equals(DocumentConstants.ISSUE_DUEDATE))
    		{
    			return i18n.getText("issue.field.duedate");
    		}
    		if(dateFieldName.equals(DocumentConstants.ISSUE_CREATED))
    		{
    			return i18n.getText("issue.field.created");
    		}
    		if(dateFieldName.equals(DocumentConstants.ISSUE_UPDATED))
    		{
    			return i18n.getText("issue.field.updated");
    		}
    		if(dateFieldName.startsWith(DocumentConstants.ISSUE_CUSTOMFIELD_PREFIX))
    		{
    			CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(dateFieldName);
    			if(customField != null) {
    				return customField.getName();
    			}
    		}
    	}

    	// fall back to raw date field name
    	return dateFieldName;
    }

    /**
     * Returns whether the field is of date-time type (versus date-only type).
     */
    public boolean isDateTimeField()
    {
    	if(dateFieldName.startsWith(DocumentConstants.ISSUE_CUSTOMFIELD_PREFIX))
    	{
    		CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(dateFieldName);
    		return (customField.getCustomFieldType() instanceof DateTimeCFType);
    	}

    	return false;
    }

	/**
     * Gets the date value used in the calendar from an issue.
     */
    public Date getDate(Issue issue)
    {

    	if(dateFieldName != null)
    	{
    		// get standard (using reflection would be pointless) or custom fields
    		if(dateFieldName.equals(DocumentConstants.ISSUE_DUEDATE))
    		{
    			return issue.getDueDate() != null ? new Date(issue.getDueDate()) : null;
    		}
    		else if(dateFieldName.equals(DocumentConstants.ISSUE_CREATED))
    		{
    			return issue.getCreated() != null ? new DateTime(issue.getCreated()) : null;
    		}
    		else if(dateFieldName.equals(DocumentConstants.ISSUE_UPDATED))
    		{
    			return issue.getUpdated() != null ? new DateTime(issue.getUpdated()) : null;
    		}
    		else if(dateFieldName.startsWith(DocumentConstants.ISSUE_CUSTOMFIELD_PREFIX))
    		{
    			Timestamp timestamp = null;
    			final CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(dateFieldName);
    			if(customField != null) {
    				Object value = customField.getValue(issue);
    				if(value instanceof Timestamp) {
    					timestamp = (Timestamp)value;
    				}
    			}
    			CustomFieldType cfType = customField.getCustomFieldType();
    			if (cfType instanceof DateTimeCFType) {
    				return timestamp != null ? new DateTime(timestamp) : null;
    			} else
    			{
    				return timestamp != null ? new Date(timestamp) : null;
    			}
    		}
    	}
    	
    	log.warn("Date field '" + dateFieldName + "' was not found, displaying 'dueDate'");

    	// fall back to 'dueDate'
    	return issue.getDueDate() != null ? new Date(issue.getDueDate()) : null;
    }
    
}
