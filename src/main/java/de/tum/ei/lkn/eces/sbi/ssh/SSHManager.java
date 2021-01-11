package de.tum.ei.lkn.eces.sbi.ssh;

/**
 * SSH manager for handling SSH connections.
 *
 * Heavily inspired from https://stackoverflow.com/questions/2405885/run-a-command-over-ssh-with-jsch
 *
 * @author cabbott
 * @author Amaury Van Bemten
 */

import com.jcraft.jsch.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;


public class SSHManager {
    private static final Logger logger = Logger.getLogger(SSHManager.class);
    private String username;
    private String host;
    private int port;
    private Session session;

    public SSHManager(String username, String host) throws JSchException {
        this(username, host, "~/.ssh/id_rsa");
    }

    public SSHManager(String username, String host, int port) throws JSchException {
        this(username, host, "~/.ssh/id_rsa", "~/.ssh/known_hosts", port);
    }

    public SSHManager(String username, String host, String privateKey) throws JSchException {
        this(username, host, privateKey, "~/.ssh/known_hosts");
    }

    public SSHManager(String username, String host, String privateKey, String knownHostsFileName) throws JSchException {
        this(username, host, privateKey, knownHostsFileName, 22);
    }

    public SSHManager(String username, String host, String privateKey, String knownHostsFileName, int port) throws JSchException {
        this(username, host, privateKey, knownHostsFileName, port, 60000);
    }

    public SSHManager(String username, String host, String privateKey, String knownHostsFileName, int port, int timeOutMilliseconds) throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(knownHostsFileName);
        jsch.addIdentity(privateKey);
        this.username = username;
        this.host = host;
        this.port = port;
        session = jsch.getSession(username, host, port);
        // TODO: get rid of this, it should work with the file name...?
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(timeOutMilliseconds);
    }

    private String logError(String errorMessage) {
        if(errorMessage != null)
            logger.error(username + "@" + host + ":" + port + " - " + errorMessage);

        return errorMessage;
    }

    private String logWarning(String warnMessage) {
        if(warnMessage != null)
            logger.warn(username + "@" + host + ":" + port + " - " +  warnMessage);

        return warnMessage;
    }

    public SSHReturn sendCommand(String command) {
        StringBuilder outputBuffer = new StringBuilder();
        StringBuilder outputErrBuffer = new StringBuilder();

        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            // Getting output
            InputStream commandOutput = channel.getInputStream();
            InputStream commandErrOutput = ((ChannelExec) channel).getErrStream();

            channel.connect();

            int readByte = commandOutput.read();
            while(readByte != 0xffffffff) {
                outputBuffer.append((char)readByte);
                readByte = commandOutput.read();
            }

            readByte = commandErrOutput.read();
            while(readByte != 0xffffffff) {
                outputErrBuffer.append((char)readByte);
                readByte = commandErrOutput.read();
            }

            channel.disconnect();
            return new SSHReturn(channel.getExitStatus(), outputBuffer.toString(), outputErrBuffer.toString());
        }
        catch(IOException | JSchException x) {
            logWarning(x.getMessage());
            return null;
        }
    }

    public void close() {
        session.disconnect();
    }
}