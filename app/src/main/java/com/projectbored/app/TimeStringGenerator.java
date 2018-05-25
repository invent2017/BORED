package com.projectbored.app;

import java.util.Calendar;



public class TimeStringGenerator {
    private Calendar time;
    private String timeString;

    public TimeStringGenerator() {
        time = Calendar.getInstance();
        setTimeString();
    }

    public TimeStringGenerator(long timeInMillis) {
        time = Calendar.getInstance();
        time.setTimeInMillis(timeInMillis);
        setTimeString();
    }

    private void setTimeString() {
         int hour = time.get(Calendar.HOUR_OF_DAY);
         int minute = time.get(Calendar.MINUTE);
         if(hour < 12) {
             if(minute < 10) {
                 timeString = new StringBuilder().append(hour).append(":0").append(minute).append(" A.M.").toString();
             } else {
                 timeString = new StringBuilder().append(hour).append(":").append(minute).append(" A.M.").toString();
             }
         } else {
             if(hour == 12) {
                 if(minute < 10) {
                     timeString = new StringBuilder().append(hour).append(":0").append(minute).append(" P.M.").toString();
                 } else {
                     timeString = new StringBuilder().append(hour).append(":").append(minute).append(" P.M.").toString();
                 }
             } else {
                 hour = hour - 12;
                 if(minute < 10) {
                     timeString = new StringBuilder().append(hour).append(":0").append(minute).append(" P.M.").toString();
                 } else {
                     timeString = new StringBuilder().append(hour).append(":").append(minute).append(" P.M.").toString();
                 }
             }
         }
    }

    public String getTimeString() {
        return timeString;
    }
}
