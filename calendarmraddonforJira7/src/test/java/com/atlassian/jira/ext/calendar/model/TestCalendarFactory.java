package com.atlassian.jira.ext.calendar.model;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.memory.MemoryPropertySet;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.TzId;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class TestCalendarFactory extends MockObjectTestCase
{

    private Set<Issue> issues;
    private Set<Version> versions;
    private PropertyList vTimezoneProperties;

    protected void setUp() throws Exception
    {
        super.setUp();

        Mock mockIssue;
        Mock mockVersion;

        PropertySet propertySet;

        User assignee;

        propertySet = new MemoryPropertySet()
        {
            public String getString(String string)
            {
                return string;
            }
        };
        propertySet.init(new HashMap(), new HashMap());

        assignee = null;
        issues = new LinkedHashSet<Issue>();

        mockIssue = new Mock(Issue.class);
        mockIssue.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(1000)));
        mockIssue.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(new Timestamp(System.currentTimeMillis())));
        mockIssue.expects(atLeastOnce()).method("getKey").withNoArguments().will(returnValue("TST-1"));
        mockIssue.expects(atLeastOnce()).method("getSummary").withNoArguments().will(returnValue("Test Issue 1"));
        mockIssue.expects(atLeastOnce()).method("getDescription").withNoArguments().will(returnValue("Test Issue 1"));
        mockIssue.expects(atLeastOnce()).method("getAssignee").withNoArguments().will(returnValue(assignee));
        issues.add((Issue) mockIssue.proxy());

        mockIssue = new Mock(Issue.class);
        mockIssue.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(1001)));
        mockIssue.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(new Timestamp(System.currentTimeMillis())));
        mockIssue.expects(atLeastOnce()).method("getKey").withNoArguments().will(returnValue("TST-2"));
        mockIssue.expects(atLeastOnce()).method("getSummary").withNoArguments().will(returnValue("Test Issue 2"));
        mockIssue.expects(atLeastOnce()).method("getDescription").withNoArguments().will(returnValue("Test Issue 2"));
        mockIssue.expects(atLeastOnce()).method("getAssignee").withNoArguments().will(returnValue(assignee));
        issues.add((Issue) mockIssue.proxy());

        versions = new LinkedHashSet<Version>();

        Mock mockProject = new Mock(Project.class);
        mockProject.expects(atLeastOnce()).method("getName").withNoArguments().will(returnValue("TST"));

        mockVersion = new Mock(Version.class);
        mockVersion.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(1000)));
        mockVersion.expects(atLeastOnce()).method("getName").withNoArguments().will(returnValue("Version 1"));
        mockVersion.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(new Date()));
        mockVersion.expects(atLeastOnce()).method("getProjectObject").withNoArguments().will(returnValue(mockProject.proxy()));
        mockVersion.expects(atLeastOnce()).method("getDescription").withNoArguments().will(returnValue("Version Description 1"));
        versions.add((Version) mockVersion.proxy());

        mockVersion = new Mock(Version.class);
        mockVersion.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(1001)));
        mockVersion.expects(atLeastOnce()).method("getName").withNoArguments().will(returnValue("Version 2"));
        mockVersion.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(new Date()));
        mockVersion.expects(atLeastOnce()).method("getProjectObject").withNoArguments().will(returnValue(mockProject.proxy()));
        mockVersion.expects(atLeastOnce()).method("getDescription").withNoArguments().will(returnValue("Version Description 2"));
        versions.add((Version) mockVersion.proxy());

        vTimezoneProperties = new PropertyList();
        vTimezoneProperties.add(new TzId(""));
    }

    public void testCreateICalendarFromSearch()
    {
        CalendarFactory calendarFactory = new CalendarFactory(null, null);
        net.fortuna.ical4j.model.Calendar calendar = calendarFactory.createICalendarFromSearch(false);

        ComponentList componentList = calendar.getComponents();
        VEvent issue1Component;
        VEvent issue2Component;
        VEvent version1Component;
        VEvent version2Component;

        assertEquals(2, componentList.size());
        issue1Component = (VEvent) componentList.get(0);
        issue2Component = (VEvent) componentList.get(1);

        assertEquals("Test Issue 1", issue1Component.getProperties().getProperty("DESCRIPTION").getValue());
        assertEquals("Test Issue 2", issue2Component.getProperties().getProperty("DESCRIPTION").getValue());

        /* With versions mapped */
        calendar = calendarFactory.createICalendarFromSearch(true);
        componentList = calendar.getComponents();

        assertEquals(4, componentList.size());
        issue1Component = (VEvent) componentList.get(0);
        issue2Component = (VEvent) componentList.get(1);
        version1Component = (VEvent) componentList.get(2);
        version2Component = (VEvent) componentList.get(3);

        assertEquals("Test Issue 1", issue1Component.getProperties().getProperty("DESCRIPTION").getValue());
        assertEquals("Test Issue 2", issue2Component.getProperties().getProperty("DESCRIPTION").getValue());
        assertEquals("Version Description 1", version1Component.getProperties().getProperty("DESCRIPTION").getValue());
        assertEquals("Version Description 2", version2Component.getProperties().getProperty("DESCRIPTION").getValue());
    }

    public void testCreateCurrentMonth()
    {
        CalendarFactory calendarFactory;

        calendarFactory = new CalendarFactory(null, null);
        assertNotNull(calendarFactory.createCurrentMonth());
    }

    protected class CalendarFactory extends com.atlassian.jira.ext.calendar.model.CalendarFactory
    {


        public CalendarFactory(final CalendarSearchRequest calendarSearchRequest, String dateFieldName)
        {
            super(calendarSearchRequest, dateFieldName);
        }

        protected Set getIssues()
        {
            return issues;
        }

        protected Set getVersions()
        {
            return versions;
        }

        protected String getBaseUrl()
        {
            return "http://localhost/jira";
        }

        protected TimeZone getTimeZone()
        {
            return new TimeZone(new VTimeZone(vTimezoneProperties));
        }
    }

}
