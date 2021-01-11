package de.tum.ei.lkn.eces.sbi.ssh;

public class SSHReturn {
    private int exitStatus;
    private String output;
    private String errOutput;

    SSHReturn(int exitStatus, String output, String errOutput) {
        this.exitStatus = exitStatus;
        this.output = output;
        this.errOutput = errOutput;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public String getOutput() {
        return output;
    }

    public String getErrOutput() {
        return errOutput;
    }
}