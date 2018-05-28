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

        int days = (int)(difference/86400000);
        difference = difference - (days * 86400000);
        int hours = (int)(difference/3600000);
        difference = difference - (hours * 3600000);
        int minutes = (int)(difference/60000);

        String differenceString = days + " days, " + hours + " hours, and " + minutes + " minutes" ;
        return differenceString;
    }
}
