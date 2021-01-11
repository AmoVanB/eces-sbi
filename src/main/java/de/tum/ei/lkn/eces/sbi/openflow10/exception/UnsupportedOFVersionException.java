package de.tum.ei.lkn.eces.sbi.openflow10.exception;

public class UnsupportedOFVersionException extends Exception {
    private short version;
    public UnsupportedOFVersionException(short version) {
        this.version = version;
    }

    public String toString() {
        return this.getClass().getSimpleName() + ": " + version;
    }
}
