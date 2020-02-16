package com.atlassian.jira.ext.calendar.model;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.mock.ofbiz.MockGenericValue;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.ofbiz.OfBizListIterator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ofbiz.core.entity.EntityExpr;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestVersionDelegator {
    @Mock
    private OfBizDelegator mockOfbizDelegator;
    @Mock
    private OfBizListIterator mockOfBizListIterator;
    @Mock
    private VersionManager mockVersionManager;
    private VersionDelegator versionDelegator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        versionDelegator = new VersionDelegator(mockOfbizDelegator, mockVersionManager);
    }

    @Test
    public void testGetVersionsList() {
        when(mockOfBizListIterator.next()).thenReturn(new MockGenericValue("Project", EasyMap.build("id", new Long(1)
        ))).thenReturn(null);
        Version mockVersion = mock(Version.class);
        when(mockVersionManager.getVersion(1L)).thenReturn(mockVersion);
        when(mockOfbizDelegator.findListIteratorByCondition(eq("Version"), isA(EntityExpr.class))).thenReturn
                (mockOfBizListIterator);

        assertEquals(Collections.EMPTY_LIST,
                versionDelegator.getVersionsList(new Date(), new Date(), Collections.<Project>emptySet()));

        Date start = new Date();
        Date end = new Date();
        Set<Project> projects = new HashSet<Project>();

        Project mockProject = mock(Project.class);
        when(mockProject.getId()).thenReturn(1L);
        projects.add(mockProject);

        assertThat(Arrays.asList(mockVersion), is(versionDelegator.getVersionsList(start, end, projects)));

        verify(mockOfBizListIterator).close();
    }
}