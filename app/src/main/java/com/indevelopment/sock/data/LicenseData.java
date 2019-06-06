package com.indevelopment.sock.data;

import com.indevelopment.sock.model.License;

import java.util.ArrayList;

public class LicenseData {
    private static final String[][] licenseString =
            {{"Gson", "Apache License, Version 2.0"},
             {"Maoni", "MIT License (MIT)"}};

    public static ArrayList<License> generateLicense() {
        ArrayList<License> licenses = new ArrayList<>();
        for(String[] l : licenseString) {
            licenses.add(new License(l[0], l[1]));
        }
        return licenses;
    }
}
