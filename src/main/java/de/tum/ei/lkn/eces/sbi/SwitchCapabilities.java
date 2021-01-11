package de.tum.ei.lkn.eces.sbi;

import de.tum.ei.lkn.eces.sbi.openflow10.components.DetectedSwitch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * Class storing the particular switch performance values: processing time, buffer size, number of queues.
 *
 * TODO: load such values from a configuration file so that users can easily add the capabilities
 * of their switches.
 *
 * @author Amaury Van Bemten
 */
public class SwitchCapabilities {
    private Set<InetAddress> dell3048Addresses;
    private Set<InetAddress> dell4048Addresses;
    private Set<InetAddress> p3290Addresses;
    private Set<InetAddress> p3297Addresses;

    public SwitchCapabilities() {
        dell3048Addresses = new HashSet<>();
        try {
            dell3048Addresses.add(InetAddress.getByName("10.162.148.71"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.72"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.73"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.74"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.75"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.76"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.77"));
            dell3048Addresses.add(InetAddress.getByName("10.162.148.78"));

            dell4048Addresses = new HashSet<>();
            dell4048Addresses.add(InetAddress.getByName("10.162.148.81"));
            dell4048Addresses.add(InetAddress.getByName("10.162.148.82"));

            p3290Addresses = new HashSet<>();
            p3290Addresses.add(InetAddress.getByName("10.162.148.67"));

            p3297Addresses = new HashSet<>();
            p3297Addresses.add(InetAddress.getByName("10.162.148.68"));
            p3297Addresses.add(InetAddress.getByName("10.162.148.69"));
        } catch (UnknownHostException e) {
            // Cannot happen
        }

    }

    /**
     * @param detectedSwitch switch
     * @return the processing time of the switch
     * @throws UnknownHostException if the switch is not known to the class
     */
    public double getProcessingTime(DetectedSwitch detectedSwitch) throws UnknownHostException {
        if(dell4048Addresses.contains(detectedSwitch.getAddress()))
            return 2.1;
        else if(dell3048Addresses.contains(detectedSwitch.getAddress()))
            return 5.2;
        else if(p3290Addresses.contains(detectedSwitch.getAddress()))
            return 4.6;
        else if(p3297Addresses.contains(detectedSwitch.getAddress()))
            return 5.0;
        else
            throw new UnknownHostException();
    }

    /**
     * @param detectedSwitch switch
     * @return the processing time overhead induced by priority queuing
     * @throws UnknownHostException if the switch is not known to the class
     */
    public double getPriorityQueuingOverhead(DetectedSwitch detectedSwitch) throws UnknownHostException {
        if(dell4048Addresses.contains(detectedSwitch.getAddress()))
            return 27;
        else if(dell3048Addresses.contains(detectedSwitch.getAddress()))
            return 6;
        else if(p3290Addresses.contains(detectedSwitch.getAddress()))
            return 9;
        else if(p3297Addresses.contains(detectedSwitch.getAddress()))
            return 9;
        else
            throw new UnknownHostException();
    }

    /**
     * @param detectedSwitch switch
     * @return the number of priority queues of the switch
     * @throws UnknownHostException if the switch is not known to the class
     */
    public int getNumberOfQueues(DetectedSwitch detectedSwitch) throws UnknownHostException {
        if(dell4048Addresses.contains(detectedSwitch.getAddress()))
            return 4;
        else if(dell3048Addresses.contains(detectedSwitch.getAddress()))
            return 4;
        else if(p3290Addresses.contains(detectedSwitch.getAddress()))
            return 8;
        else if(p3297Addresses.contains(detectedSwitch.getAddress()))
            return 8;
        else
            throw new UnknownHostException();
    }

    /**
     * @param detectedSwitch switch
     * @param maxPorts the maximum number of ports that will be used (as this impacts the available buffer per queue)
     * @return the per port buffer size of the switch
     * @throws UnknownHostException if the switch is not known to the class
     */
    public double getPerQueueBufferSize(DetectedSwitch detectedSwitch, int maxPorts) throws UnknownHostException {
        if(dell4048Addresses.contains(detectedSwitch.getAddress())) {
            // This bytes is for 790 packets, should be remeasured for smallest packets as they are the worst
            // for fragmentation...
            switch(maxPorts) {
                case 1:
                    return 2337*790;
                case 2:
                    return 1234*790;
                case 3:
                    return 839*790;
                case 4:
                    return 635*790;
                case 5:
                    return 501*790;
                case 6:
                    return 428*790;
                default:
                    // The ugly computation: full size divided by N_QUEUES
                    return ((double) 12*1e6) / ((double) maxPorts) / ((double) getNumberOfQueues(detectedSwitch));
            }
        }
        else if(dell3048Addresses.contains(detectedSwitch.getAddress())) {
            // This bytes is for big packets, should be remeasured for smallest packets as they are the worst
            // for fragmentation...
            switch(maxPorts) {
                case 1:
                    return 339*790;
                case 2:
                    return 203*790;
                case 3:
                    return 146*790;
                case 4:
                    return 114*790;
                case 5:
                    return 92*790;
                case 6:
                    return 79*790;
                default:
                    // The ugly computation: full size divided by N_QUEUES
                    return ((double) 4*1e6) / ((double) maxPorts) / ((double) getNumberOfQueues(detectedSwitch));
            }
        }
        else if(p3290Addresses.contains(detectedSwitch.getAddress())) {
            // This bytes is for big packets, should be remeasured for smallest packets as they are the worst
            // for fragmentation...
            switch(maxPorts) {
                case 1:
                    return 230*1516;
                case 2:
                    return 124*1516;
                case 3:
                    return 84*1516;
                case 4:
                    return 64*1516;
                case 5:
                    return 53*1516;
                case 6:
                    return 49*1516;
                default:
                    // The ugly computation: full size divided by N_QUEUES
                    return ((double) 4*1e6) / ((double) maxPorts) / ((double) getNumberOfQueues(detectedSwitch));
            }
        }
        else if(p3297Addresses.contains(detectedSwitch.getAddress())) {
            // This bytes is for big packets, should be remeasured for smallest packets as they are the worst
            // for fragmentation...
            switch(maxPorts) {
                case 1:
                    return 230*1516;
                case 2:
                    return 124*1516;
                case 3:
                    return 84*1516;
                case 4:
                    return 64*1516;
                case 5:
                    return 53*1516;
                case 6:
                    return 49*1516;
                default:
                    // The ugly computation: full size divided by N_QUEUES
                    return ((double) 4*1e6) / ((double) maxPorts) / ((double) getNumberOfQueues(detectedSwitch));
            }
        }
        else
            throw new UnknownHostException();
    }
}
