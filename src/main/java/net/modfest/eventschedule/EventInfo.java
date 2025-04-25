package net.modfest.eventschedule;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.Date;

public class EventInfo {
    @SerializedName("title")
    public String name;
    @SerializedName("start")
    public Instant start;
    @SerializedName("end")
    Instant end;

    @Override
    public String toString() {
        return "EventInfo{" +
                "name='" + name + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}