package com.indevelopment.sock.model;

public class License {
    private String libraryName, libraryLicense;

    public License(String libraryName, String libraryLicense) {
        this.libraryName = libraryName;
        this.libraryLicense = libraryLicense;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getLibraryLicense() {
        return libraryLicense;
    }

    public void setLibraryLicense(String libraryLicense) {
        this.libraryLicense = libraryLicense;
    }
}
