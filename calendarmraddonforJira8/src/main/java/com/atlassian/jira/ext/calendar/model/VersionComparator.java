package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.project.version.Version;

import java.util.Comparator;

public class VersionComparator  implements Comparator<Version>
{

    /**
     * Compares two versions and orders them based on the due date and then based on Id.
     */
    public int compare(Version version1, Version version2)
    {
        if(version1 == version2)
        {
            return 0;
        }

        if (null != version1.getReleaseDate() && null == version2.getReleaseDate()) return 1;
        if (null == version1.getReleaseDate() && null != version2.getReleaseDate()) return -1;

        if(version1.getReleaseDate() != null && version2.getReleaseDate() != null)
        {
            int releaseDateComparison = version1.getReleaseDate().compareTo(version2.getReleaseDate());
            if(releaseDateComparison != 0)
            {
                return releaseDateComparison;
            }
        }
//        else if(version2.getReleaseDate() != null)
//        {
//            return -1;
//        }

        return version1.getId().compareTo(version2.getId());
    }
}
