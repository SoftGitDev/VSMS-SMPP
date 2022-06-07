package com.softtech.smpp.util;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Absolute time formatter is a {@link TimeFormatter} implementation referred to in
 * SMPP Protocol Specification v3.4 point 7.1.1.
 * 
 * @author SUTHAR
 * 
 */
public class AbsoluteTimeFormatter implements TimeFormatter {
    /**
     * Time/Date ASCII format for Absolute Time Format is:
     * 
     * YYMMDDhhmmsstnnp (refer for SMPP Protocol Specification v3.4)
     */
    private static final String DATE_FORMAT = "{0,number,00}{1,number,00}{2,number,00}{3,number,00}{4,number,00}{5,number,00}{6,number,0}{7,number,00}{8}";
    
    public String format(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        int year = calendar.get(Calendar.YEAR) - 2000;
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int tenthsOfSecond = calendar.get(Calendar.MILLISECOND) / 100;
        
        int offset = calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
        
        // Get the sign
        char sign;
        if (offset >= 0) {
            sign = '+';
        } else {
            sign = '-';
        }
        
        // Time difference in quarter hours
        int timeDiff = Math.abs(offset) / (15 * 60 * 1000);
        
        return format(year, month, day, hour, minute, second, tenthsOfSecond, timeDiff, sign);
    }
    
    public String format(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return format(cal);
    }
    
    public static final String format(Integer year, Integer month,
            Integer day, Integer hour, Integer minute, Integer second,
            int tenthsOfSecond, int timeDiff, Character sign) {
        Object[] args = new Object[] {year, month, day, hour, minute, second, tenthsOfSecond, timeDiff, sign};
        return MessageFormat.format(DATE_FORMAT, args);
    }
}
