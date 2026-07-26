package com.safenet.dto;

public class UpdatePreferencesRequest {
    private String language;
    private String timeZone;
    private String dateFormat;
    private Boolean darkMode;

    public String  getLanguage()   { return language; }
    public void    setLanguage(String v)   { language = v; }
    public String  getTimeZone()   { return timeZone; }
    public void    setTimeZone(String v)   { timeZone = v; }
    public String  getDateFormat() { return dateFormat; }
    public void    setDateFormat(String v) { dateFormat = v; }
    public Boolean getDarkMode()   { return darkMode; }
    public void    setDarkMode(Boolean v)  { darkMode = v; }
}
