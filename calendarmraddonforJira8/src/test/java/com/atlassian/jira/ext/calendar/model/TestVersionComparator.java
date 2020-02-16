package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.project.version.Version;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestVersionComparator extends MockObjectTestCase {

    public void testCompare() throws ParseException {
        Mock mockVersion1;
        Mock mockVersion2;
        Version version1;
        Version version2;
        Date releaseDate1;
        Date releaseDate2;
        VersionComparator versionComparator = new VersionComparator();

        assertEquals(0, versionComparator.compare(null, null));

        releaseDate1 = new SimpleDateFormat("ddMMyyyy").parse("19700101");
        releaseDate2 = null;

        mockVersion1 = new Mock(Version.class);
        mockVersion1.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(releaseDate1));
        version1 = (Version) mockVersion1.proxy();

        mockVersion2 = new Mock(Version.class);
        mockVersion2.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(releaseDate2));
        version2 = (Version) mockVersion2.proxy();

        assertEquals(1,versionComparator.compare(version1, version2));
        assertEquals(-1,versionComparator.compare(version2, version1));

        releaseDate2 = new Date();
        mockVersion2.reset();
        mockVersion2.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(releaseDate2));
        version2 = (Version) mockVersion2.proxy();

        assertEquals(
                releaseDate1.compareTo(releaseDate2),
                versionComparator.compare(version1, version2));
        assertEquals(
                releaseDate2.compareTo(releaseDate1),
                versionComparator.compare(version2, version1));




        mockVersion1 = new Mock(Version.class);
        mockVersion1.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(releaseDate1));
        mockVersion1.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(1)));
        version1 = (Version) mockVersion1.proxy();

        mockVersion2 = new Mock(Version.class);
        mockVersion2.expects(atLeastOnce()).method("getReleaseDate").withNoArguments().will(returnValue(releaseDate1));
        mockVersion2.expects(atLeastOnce()).method("getId").withNoArguments().will(returnValue(new Long(2)));
        version2 = (Version) mockVersion2.proxy();

        assertEquals(new Long(1).compareTo(new Long(2)), versionComparator.compare(version1, version2));
        assertEquals(new Long(2).compareTo(new Long(1)), versionComparator.compare(version2, version1));
        assertEquals(0, versionComparator.compare(version1, version1));
        assertEquals(0, versionComparator.compare(version2, version2));
    }
}
