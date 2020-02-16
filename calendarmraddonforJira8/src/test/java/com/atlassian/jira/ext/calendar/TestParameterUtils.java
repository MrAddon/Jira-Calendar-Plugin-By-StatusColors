package com.atlassian.jira.ext.calendar;

import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TestParameterUtils extends TestCase {

    public void testGetBoolean() {
        assertTrue(ParameterUtils.getBoolean(null));
        assertTrue(ParameterUtils.getBoolean("true"));
        assertTrue(ParameterUtils.getBoolean("True"));
        assertFalse(ParameterUtils.getBoolean("false"));
        assertFalse(ParameterUtils.getBoolean("False"));
        assertFalse(ParameterUtils.getBoolean("foo"));
    }

    public void testGetDuration() {
        assertNull(ParameterUtils.getDuration(null));
        assertNull(ParameterUtils.getDuration("NaN"));
        assertEquals(new Long(0),
                ParameterUtils.getDuration("0"));
    }

    public void testGetLong() {
        assertNull(ParameterUtils.getLong(null));
        assertNull(ParameterUtils.getLong("NaN"));
        assertEquals(new Long(0),
                ParameterUtils.getLong("0"));
    }

    public void testGetMonth() {
        assertNull(ParameterUtils.getMonth(null));
        assertNull(ParameterUtils.getMonth("Invalid Value"));
        assertEquals(
                "011970",
                new SimpleDateFormat("MMyyyy").format(ParameterUtils.getMonth("011970")));
    }

    public void testGetEndOfMonth() throws ParseException {
        Date now = new SimpleDateFormat("ddMMyyyy").parse("01011970");
        Date endOfMonth = ParameterUtils.endOfMonth(now);

        Calendar cal = Calendar.getInstance();
        cal.setTime(endOfMonth);

        assertEquals(31, cal.getMaximum(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.getMaximum(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.getMaximum(Calendar.MINUTE));
        assertEquals(59, cal.getMaximum(Calendar.SECOND));
        assertEquals(999, cal.getMaximum(Calendar.MILLISECOND));
    }

    public void testGetMonthStr() {
        final Date now = new Date();

        assertEquals(
                new SimpleDateFormat("MMyyyy").format(now),
                ParameterUtils.getMonthStr(now));
    }

    public void testCloneAddMonths() throws ParseException {
        final Calendar cloneSource = Calendar.getInstance();
        final int currentMonths;

        cloneSource.setTime(new SimpleDateFormat("MMyyyy").parse("061970"));
        currentMonths = cloneSource.get(Calendar.MONTH);


        Calendar modifiedCal;
        modifiedCal = ParameterUtils.cloneAddMonths(cloneSource, 3);
        assertEquals(currentMonths + 3, modifiedCal.get(Calendar.MONTH));
        
        modifiedCal = ParameterUtils.cloneAddMonths(cloneSource, -3);
        assertEquals(currentMonths - 3, modifiedCal.get(Calendar.MONTH));
    }
}
