package com.projectbored.app;

//Gets the difference between two times and gives the result in a readable String.

import java.util.Calendar;

public class TimeDifferenceGenerator {
    private long time1, time2;

    public TimeDifferenceGenerator(long time1, long time2) {
        this.time1 = time1;
        this.time2 = time2;
    }

    public String getDifference() {
        long difference = Math.abs(time1 - time2);
        Calendar differenceCalendar = Calendar.getInstance();
        differenceCalendar.setTimeInMillis(difference);

        int days = differenceCalendar.get(Calendar.DAY_OF_YEAR);
        int hours = differenceCalendar.get(Calendar.HOUR);
        int minutes = differenceCalendar.get(Calendar.MINUTE);

        String differenceString = days + " days, " + hours + " hours, and " + minutes + "minutes" ;
        return differenceString;
    }
}
