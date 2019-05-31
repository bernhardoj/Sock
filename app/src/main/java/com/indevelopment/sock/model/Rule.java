package com.indevelopment.sock.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Rule implements Parcelable {
    public final static int ACTION_MUTE_ALL = 0;
    public final static int ACTION_SHUTDOWN = 1;

    private String ruleName, day, startRule;
    private int dayIdx, requestCode;
    private boolean[] actions;
    private boolean isSwitched, isRepeating;

    public Rule(String ruleName, String day, String startRule, boolean[] actions, boolean isSwitched, int dayIdx, int requestCode, boolean isRepeating) {
        this.ruleName = ruleName;
        this.startRule = startRule;
        this.day = day;
        this.actions = actions;
        this.isSwitched = isSwitched;
        this.dayIdx = dayIdx;
        this.requestCode = requestCode;
        this.isRepeating = isRepeating;
    }

    private Rule(Parcel in) {
        ruleName = in.readString();
        day = in.readString();
        startRule = in.readString();
        dayIdx = in.readInt();
        requestCode = in.readInt();
        isSwitched = in.readByte() != 0;
        isRepeating = in.readByte() != 0;
        actions = in.createBooleanArray();
    }

    public static final Creator<Rule> CREATOR = new Creator<Rule>() {
        @Override
        public Rule createFromParcel(Parcel in) {
            return new Rule(in);
        }

        @Override
        public Rule[] newArray(int size) {
            return new Rule[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ruleName);
        dest.writeString(day);
        dest.writeString(startRule);
        dest.writeInt(dayIdx);
        dest.writeInt(requestCode);
        dest.writeByte((byte) (isSwitched ? 1 : 0));
        dest.writeByte((byte) (isRepeating ? 1 : 0));
        dest.writeBooleanArray(actions);
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getStartRule() {
        return startRule;
    }

    public void setStartRule(String startRule) {
        this.startRule = startRule;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public boolean isSwitched() {
        return isSwitched;
    }

    public void setSwitched(boolean switched) {
        isSwitched = switched;
    }

    public int getDayIdx() {
        return dayIdx;
    }

    public void setDayIdx(int dayIdx) {
        this.dayIdx = dayIdx;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public boolean isRepeating() {
        return isRepeating;
    }

    public void setRepeating(boolean repeating) {
        isRepeating = repeating;
    }

    public boolean getActions(int idx) {
        return actions[idx];
    }

    public boolean[] getAllActions() {
        return actions;
    }

    public void setActions(int idx, boolean state) {
        actions[idx] = state;
    }
}
