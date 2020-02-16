package com.atlassian.jira.ext.calendar.model;

import com.atlassian.jira.ofbiz.OfBizDelegator;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class MockVersionDelegator extends VersionDelegator {

    private List versionsList;

    public MockVersionDelegator(OfBizDelegator ofbizDelegator) {
        super(ofbizDelegator, null);
    }

    public List getVersionsList(Date startDate, Date endDate, Set projects) {
        return versionsList;
    }

    public void setVersionsList(List versionsList) {
        this.versionsList = versionsList;
    }
}
