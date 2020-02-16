package com.atlassian.jira.ext.calendar.model;

import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestGregorianCalendarFactory extends TestCase {

    public void testConstructCalendar() throws ParseException {
        assertEquals(1, GregorianCalendarFactory.constructCalendar().getMinimalDaysInFirstWeek());

        Date d = new SimpleDateFormat("ddMMyyyy").parse("01011970");
        assertEquals(d, GregorianCalendarFactory.constructCalendar(d).getTime());
    }
}
