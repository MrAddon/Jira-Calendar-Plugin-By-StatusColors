package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.version.Version;
import com.google.common.base.Objects;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.CategoryList;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.XProperty;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * A class for handling the production of Calendars.
 */
public class CalendarFactory
{
    private static final Logger LOG = Logger.getLogger(CalendarFactory.class);
    private final CalendarSearchRequest search;
    private final DateFieldProvider dateFieldProvider;
    private final TimeZone timezone;

    public CalendarFactory(CalendarSearchRequest search, String dateFieldName)
    {
        this.search = search;
        this.dateFieldProvider = new DateFieldProvider(dateFieldName);
        this.timezone = Objects.firstNonNull(
                TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(TimeZone.getDefault().getID()),
                TimeZone.getDefault()
        );
    }

    /**
     * Creates an ICal calendar from the search request
     * @param showReleaseDates - Whether release dates are to be shown in the ICal calendar
     * @return An ICal calendar
     */
    public Calendar createICalendarFromSearch(boolean showReleaseDates)
    {
        ComponentList calendarComponents = new ComponentList();
        
        calendarComponents.addAll(mapIssuesAsEvents());
        if(showReleaseDates) 
        {
            calendarComponents.addAll(mapVersionsAsEvents());
        }

        return createCalendar(calendarComponents);
    }

    /**
     * Returns a MonthEvents object based on the given SearchRequest object
     * @param monthStart The starting day of the month
     */
    public MonthEvents createMonth(java.util.Calendar monthStart)
    {
        return new MonthEvents(new ArrayList(getIssues()), new ArrayList(getVersions()), monthStart, dateFieldProvider);
    }

    /**
     * Returns a monthEvents for the current month
     */
    public MonthEvents createCurrentMonth()
    {
        java.util.Calendar calendar = GregorianCalendarFactory.constructCalendar();
        int dom = calendar.get(java.util.Calendar.DAY_OF_MONTH) - 1;
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -dom);

        return createMonth(calendar);
    }

    /**
     * Takes the list of issues and makes them into a list of  ICal Events
     * @return a list of ICal events
     */
    private List mapIssuesAsEvents()
    {
        List eventList = new ArrayList();
        for (Iterator iterator = getIssues().iterator(); iterator.hasNext();)
        {
            Issue issue = (Issue) iterator.next();
            eventList.add(createCalendarIssue(issue));
        }
        return eventList;
    }

    ///CLOVER:OFF
    protected Set getIssues()
    {
        return search.getIssues();
    }

    protected Set getVersions()
    {
        return search.getVersions();
    }
    
    protected TimeZone getTimeZone()
    {
    	return timezone;
    }
    ///CLOVER:ON

    /**
     * Takes the list of Versions for the projects involved in the search
     * and converts them into ICal events.
     * @return A list of ICal events
     */
    private List mapVersionsAsEvents()
    {
        List eventList = new ArrayList();
        for (Iterator iterator = getVersions().iterator(); iterator.hasNext();)
        {
            Version version = (Version) iterator.next();
            eventList.add(createCalendarVersion(version));
        }
        return eventList;
    }

    /**
     * Converts a JIRA Issue into an ICal calendar component
     * @param issue the issue to be converted
     * @return an ICal calendar component
     */
    private Component createCalendarIssue(Issue issue)
    {
    	Date beginDate = dateFieldProvider.getDate(issue);
    	Date endDate = null; // set end date to due date + 24 hours to make clients happy (should not be explicit)
    	if (beginDate instanceof DateTime) {
    		endDate = new DateTime(beginDate.getTime() + DateUtils.MILLIS_PER_MINUTE * 30);
    	} 
    	else {
    		endDate = new Date(beginDate.getTime() + DateUtils.MILLIS_PER_DAY); // Event lasts for 24 hours - full day event
    	}
    	
    	VEvent event = new VEvent(beginDate, endDate, getTitle(issue));
        PropertyList eventProperties = event.getProperties();
        if(beginDate instanceof DateTime) {
        	eventProperties.getProperty(Property.DTSTART).getParameters().add(Value.DATE_TIME);
        } 
        else {
        	eventProperties.getProperty(Property.DTSTART).getParameters().add(Value.DATE);
        }
        if (endDate instanceof DateTime) {
        	eventProperties.getProperty(Property.DTEND).getParameters().add(Value.DATE_TIME);
        } 
        else {
        	eventProperties.getProperty(Property.DTEND).getParameters().add(Value.DATE);
        }
        eventProperties.add(new Uid("issue-" + issue.getId()+ "@" + getBaseUrl()));
        eventProperties.add(getDescription(issue));
        eventProperties.add(new Categories(new CategoryList("Issues")));
        eventProperties.add(Status.VEVENT_CONFIRMED);
        eventProperties.add(getUrl(issue));
        
        try
        {
            eventProperties.add(getAttendee(issue));
        }
        catch (Exception e)
        {
            LOG.error("CalendarFactory.createCalendarIssue(Issue):Component Error creating attendee.", e);
        }
        return event;
    }

    /**
     * Produces an ICal description for a Jira Issue
     * @param issue
     * @return an ICal description for the issue
     */
    private Description getDescription(Issue issue)
    {
        return new Description(issue.getDescription());
    }

    /**
     * Produces the Title for ICal of a Jira Issue
     * @param issue
     * @return the Title for the issue
     */
    private String getTitle(Issue issue)
    {
        return "[" + issue.getKey() + "] " + issue.getSummary();
    }

    /**
     * Produces a URL for ICal of a Jira Issue
     * @param issue
     * @return the URL to access the issue
     */
    private Url getUrl(Issue issue)
    {
        try
        {
            String baseUrl = getBaseUrl();

            return new Url(new URI(baseUrl + "/browse/" + issue.getKey()));
        }
        catch (URISyntaxException use)
        {
            return new Url();
        }
    }

    protected String getBaseUrl()
    {
        return ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
    }

    /**
     * Creates an ICal component from a Jira Version
     * @param version The Jira Version to be converted
     * @return The Ical componenet based on the Jira Version
     */
    private Component createCalendarVersion(Version version)
    {
        /* The null check is almost not needed due to the way VersionDelegator#getVersionsList works */
        if (version.getReleaseDate() != null)
        {
        	// set end date to release date + 24 hours to make clients happy (should not be explicit)

        	Date beginDate = new Date(version.getReleaseDate());
        	Date endDate = new Date(beginDate.getTime() + DateUtils.MILLIS_PER_DAY);
        	VEvent event = new VEvent(beginDate, endDate, getTitle(version));
        	
        	PropertyList eventProperties = event.getProperties();
            if(beginDate instanceof DateTime) {
            	eventProperties.getProperty(Property.DTSTART).getParameters().add(Value.DATE_TIME);
            }
            else {
            	eventProperties.getProperty(Property.DTSTART).getParameters().add(Value.DATE);
            }
            if (endDate instanceof DateTime) {
            	eventProperties.getProperty(Property.DTEND).getParameters().add(Value.DATE_TIME);
            } 
            else {
            	eventProperties.getProperty(Property.DTEND).getParameters().add(Value.DATE);
            }
            eventProperties.add(new Uid("version-" + version.getId()+ "@" + getBaseUrl()));
            eventProperties.add(getDescription(version));
            eventProperties.add(new Categories(new CategoryList("Issues")));
            eventProperties.add(Status.VEVENT_CONFIRMED);
            return event;
        }
        else
        {
            return null;
        }
    }

    private Attendee getAttendee(Issue issue) throws URISyntaxException
    {
        ParameterList paramList = new ParameterList();
        paramList.add(new Cn(issue.getAssignee().getDisplayName()));
        return new Attendee(paramList, "mailto:" + issue.getAssignee().getEmailAddress());
    }

    /**
     * Creates a Title for a Jira Version
     * @param version
     * @return The title for that version
     */
    private String getTitle(Version version)
    {
        return "[" + version.getProjectObject().getName() + "] " + version.getName();
    }

    /**
     * Creates an ICal description for a Jira Version
     * @param version
     * @return the Ical description for the Jira Version
     */
    private Description getDescription(Version version)
    {
        return new Description(version.getDescription());
    }


    /**
     * Creates an ICal Calendar containing the given components
     * @param calendarComponents
     * @return A new ICal Calendar
     */
    private Calendar createCalendar(ComponentList calendarComponents)
    {
        PropertyList calendarProperties = new PropertyList();

        calendarProperties.add(new ProdId("-//JIRA//iCal4j 1.0//EN"));
        calendarProperties.add(net.fortuna.ical4j.model.property.Version.VERSION_2_0);
        calendarProperties.add(CalScale.GREGORIAN);
        calendarProperties.add(Method.PUBLISH);
        calendarProperties.add(new XProperty("X-WR-TIMEZONE", new ParameterList(), timezone.getID()));

        return new Calendar(calendarProperties, calendarComponents);
    }
   
}
