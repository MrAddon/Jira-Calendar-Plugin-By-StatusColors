package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.priority.Priority;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TestIssueComparator extends MockObjectTestCase {

    public void testCompare() throws ParseException {
        Mock mockIssue1;
        Mock mockIssue2;
        Mock mockIssue1Priority;
        Mock mockIssue2Priority;

        Issue issue1;
        Issue issue2;
        Timestamp issue1DueDate = new Timestamp(new SimpleDateFormat("ddMMyyyy").parse("01011970").getTime());
        Timestamp issue2DueDate = new Timestamp(new SimpleDateFormat("ddMMyyyy").parse("02011970").getTime());
        Priority issue1Priority;
        Priority issue2Priority;

        mockIssue1Priority = new Mock(Priority.class);
        mockIssue1Priority.expects(atLeastOnce()).method("compareTo").with(isA(Priority.class)).will(returnValue(1));
        issue1Priority = (Priority) mockIssue1Priority.proxy();

        mockIssue1 = new Mock(Issue.class);
        mockIssue1.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        issue1 = (Issue) mockIssue1.proxy();

        mockIssue2Priority = new Mock(Priority.class);
        mockIssue2Priority.expects(atLeastOnce()).method("compareTo").with(isA(Priority.class)).will(returnValue(1));
        issue2Priority = (Priority) mockIssue2Priority.proxy();

        mockIssue2 = new Mock(Issue.class);
        mockIssue2.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue2DueDate));
        issue2 = (Issue) mockIssue2.proxy();


        IssueComparator issueComparator = new IssueComparator(DocumentConstants.ISSUE_DUEDATE);

        /* Test comparison using issue due date */
        assertEquals(0, issueComparator.compare(null, null));
        assertEquals(0, issueComparator.compare(issue1, issue1));
        assertEquals(0, issueComparator.compare(issue2, issue2));

        assertEquals(
                issue1DueDate.compareTo(issue2DueDate),
                issueComparator.compare(issue1, issue2)
        );
        assertEquals(
                issue2DueDate.compareTo(issue1DueDate),
                issueComparator.compare(issue2, issue1)
        );

        mockIssue2.reset();
        mockIssue2.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(null));
        issue2 = (Issue) mockIssue2.proxy();

        assertEquals(1, issueComparator.compare(issue1, issue2));
        assertEquals(-1, issueComparator.compare(issue2, issue1));

        /* Test comparison using issue priority */
        mockIssue1.reset();
        mockIssue1.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        mockIssue1.expects(atLeastOnce()).method("getPriorityObject").withNoArguments().will(returnValue(issue1Priority));
        issue1 = (Issue) mockIssue1.proxy();

        mockIssue2.reset();
        mockIssue2.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        mockIssue2.expects(atLeastOnce()).method("getPriorityObject").withNoArguments().will(returnValue(issue2Priority));
        issue2 = (Issue) mockIssue2.proxy();

        assertEquals(1, issueComparator.compare(issue1, issue2));
        assertEquals(1, issueComparator.compare(issue2, issue1));

        mockIssue1.reset();
        mockIssue1.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        mockIssue1.expects(atLeastOnce()).method("getPriorityObject").withNoArguments().will(returnValue(null));
        issue1 = (Issue) mockIssue1.proxy();

        mockIssue2.reset();
        mockIssue2.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        mockIssue2.expects(atLeastOnce()).method("getPriorityObject").withNoArguments().will(returnValue(issue2Priority));
        issue2 = (Issue) mockIssue2.proxy();

        assertEquals(-1, issueComparator.compare(issue1, issue2));
        assertEquals(1, issueComparator.compare(issue2, issue1));

        mockIssue1.reset();
        mockIssue1.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        mockIssue1.expects(atLeastOnce()).method("getPriorityObject").withNoArguments().will(returnValue(issue1Priority));
        mockIssue1.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(1L)));
        issue1 = (Issue) mockIssue1.proxy();

        mockIssue2.reset();
        mockIssue2.expects(atLeastOnce()).method("getDueDate").withNoArguments().will(returnValue(issue1DueDate));
        mockIssue2.expects(atLeastOnce()).method("getPriorityObject").withNoArguments().will(returnValue(issue1Priority));
        mockIssue2.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(2L)));
        issue2 = (Issue) mockIssue2.proxy();

        assertEquals(
                new Long(1).compareTo(new Long(2)),
                issueComparator.compare(issue1, issue2)
        );

        assertEquals(
                new Long(2).compareTo(new Long(1)),
                issueComparator.compare(issue2, issue1)
        );
    }
}
