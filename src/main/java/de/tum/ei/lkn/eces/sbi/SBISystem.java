package de.tum.ei.lkn.eces.sbi;

import com.jcraft.jsch.JSchException;
import de.tum.ei.lkn.eces.core.ComponentStatus;
import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.RootSystem;
import de.tum.ei.lkn.eces.core.annotations.ComponentStateIs;
import de.tum.ei.lkn.eces.core.annotations.HasComponent;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.network.*;
import de.tum.ei.lkn.eces.network.mappers.LinkMapper;
import de.tum.ei.lkn.eces.network.mappers.ToNetworkMapper;
import de.tum.ei.lkn.eces.network.util.NetworkInterface;
import de.tum.ei.lkn.eces.routing.mappers.PathMapper;
import de.tum.ei.lkn.eces.routing.responses.Path;
import de.tum.ei.lkn.eces.sbi.mappers.*;
import de.tum.ei.lkn.eces.sbi.openflow10.OFController;
import de.tum.ei.lkn.eces.sbi.openflow10.components.DetectedLink;
import de.tum.ei.lkn.eces.sbi.openflow10.components.DetectedSwitch;
import de.tum.ei.lkn.eces.sbi.openflow10.components.OFSwitch;
import de.tum.ei.lkn.eces.sbi.openflow10.message.OFMessageFactory;
import de.tum.ei.lkn.eces.sbi.openflow10.message.OFPacketOutMessage;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFMatchStructure;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.OFPortStructure;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionEnqueue;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionOutput;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionStripVlan;
import de.tum.ei.lkn.eces.sbi.openflow10.message.structure.actions.OFActionStructure;
import de.tum.ei.lkn.eces.sbi.openflow10.util.LLDPUtils;
import de.tum.ei.lkn.eces.sbi.ssh.SSHManager;
import de.tum.ei.lkn.eces.sbi.ssh.SSHReturn;
import de.tum.ei.lkn.eces.tenantmanager.Flow;
import de.tum.ei.lkn.eces.tenantmanager.VirtualMachine;
import de.tum.ei.lkn.eces.tenantmanager.mappers.VirtualMachineMapper;
import de.tum.ei.lkn.eces.tenantmanager.matching.FiveTupleMatching;
import de.tum.ei.lkn.eces.tenantmanager.matching.Matching;
import de.tum.ei.lkn.eces.tenantmanager.rerouting.mappers.RerouteFromMapper;
import de.tum.ei.lkn.eces.tenantmanager.traffic.TokenBucketTrafficContract;
import de.tum.ei.lkn.eces.tenantmanager.traffic.TrafficContract;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Southbound interface (SBI) system responsible for discovering switches and the topology and to configure switches
 * and hosts for flows forwarding.
 *
 * The system creates the discovered topology using the networking system and configures flows that are added by
 * the routing system.
 *
 * @author Amaury Van Bemten
 */
public class SBISystem extends RootSystem {
    private static final int OF_PORT_NUMBER = 6633;
    private static final int CONTROL_VM_PORT = 20000;
    private Map<InetAddress, DetectedSwitch> connectedSwitches;
    private Map<InetAddress, NetworkNode> switchToNode;
    private Map<Link, Integer> linksToSourcePortId;
    private Set<InetAddress> expectedSwitches;
    private NetworkingSystem networkingSystem;
    private Network network;
    private SwitchCapabilities switchCapabilities;
    private Set<ExpectedHost> expectedHosts;
    private VMIdMapper vmIdMapper;
    private VMIdListMapper vmIdListMapper;
    private RuleIdMapper ruleIdMapper;
    private RuleIdListMapper ruleIdListMapper;
    private PathMapper pathMapper;
    private VirtualMachineMapper virtualMachineMapper;
    private RerouteFromMapper rerouteFromMapper;
    private ToNetworkMapper toNetworkMapper;
    private LinkMapper linkMapper;
    private int vmsPerServer;
    private int rulesPerVm;
    private Set<String> controlVmCreated;
    private boolean startupFinished;
    private boolean waitForVmCreation;

    /**
     * @param controller controller to use
     * @param networkingSystem networking system to use
     * @param network network to which switches should be added
     * @param hosts set of hosts to contact/accept
     * @param switches set of switches to accept
     * @param vmsPerServer number of VMs per server
     */
    public SBISystem(Controller controller, NetworkingSystem networkingSystem, Network network, Set<ExpectedHost> hosts, Set<InetAddress> switches, int vmsPerServer, int rulesPerVm, boolean waitForVmCreation) {
        super(controller);
        this.networkingSystem = networkingSystem;
        this.network = network;
        this.connectedSwitches = new HashMap<>();
        this.switchToNode = new HashMap<>();
        this.linksToSourcePortId = new HashMap<>();
        this.expectedSwitches = switches;
        this.switchCapabilities = new SwitchCapabilities();
        this.vmIdMapper = new VMIdMapper(controller);
        this.vmIdListMapper = new VMIdListMapper(controller);
        this.ruleIdMapper = new RuleIdMapper(controller);
        this.ruleIdListMapper = new RuleIdListMapper(controller);
        this.virtualMachineMapper = new VirtualMachineMapper(controller);
        this.pathMapper = new PathMapper(controller);
        this.rerouteFromMapper = new RerouteFromMapper(controller);
        this.toNetworkMapper = new ToNetworkMapper(controller);
        this.linkMapper = new LinkMapper(controller);
        this.vmsPerServer = vmsPerServer;
        this.rulesPerVm = rulesPerVm;
        this.startupFinished = false;
        this.waitForVmCreation = waitForVmCreation;

        this.controlVmCreated = new HashSet<>();
        this.expectedHosts = checkDependenciesOnExpectedHostsAndInitialize(hosts);

        // Start the OpenFlow controller
        new Thread(new OFController(controller, OF_PORT_NUMBER, expectedSwitches)).start();
    }

    private Set<ExpectedHost> checkDependenciesOnExpectedHostsAndInitialize(Set<ExpectedHost> expectedHosts) {
        logger.info("Checking and initializing hosts");
        Set<ExpectedHost> hostsWithVerifiedDependencies = new HashSet<>();
        for(ExpectedHost expectedHost : expectedHosts) {
            // Can we connect to the host?
            SSHManager ssh;
            try {
                ssh = new SSHManager("root", expectedHost.getAddress().getCanonicalHostName());
            } catch (JSchException e) {
                logger.error("Impossible to SSH to " + expectedHost.getAddress() + ": " + e + ". Ignoring host!");
                continue;
            }

            SSHReturn sshReturn;

            // Does the host have Docker and Vagrant installed?
            sshReturn = ssh.sendCommand("docker --version");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": docker is not installed on the host, ignoring host!");
                continue;
            }

            sshReturn = ssh.sendCommand("vagrant --version");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": docker is not installed on the host, ignoring host!");
                continue;
            }

            // Are our DPDK scripts there?
            sshReturn = ssh.sendCommand("[ -f /usr/bin/start-dpdk-tagging ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": our DPDK scripts are not there, ignoring host!");
                continue;
            }

            sshReturn = ssh.sendCommand("[ -f /usr/bin/stop-dpdk-tagging ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": our DPDK scripts are not there, ignoring host!");
                continue;
            }

            // Are our vagrant scripts there?
            sshReturn = ssh.sendCommand("[ -f /usr/bin/create-vm ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": our vagrant scripts are not there, ignoring host!");
                continue;
            }

            sshReturn = ssh.sendCommand("[ -f /usr/bin/delete-vm ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": our vagrant scripts are not there, ignoring host!");
                continue;
            }

            sshReturn = ssh.sendCommand("[ -f /usr/bin/list-vms ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": our vagrant scripts are not there, ignoring host!");
                continue;
            }

            // Is Python3 installed?
            sshReturn = ssh.sendCommand("python3 --version");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": Python 3 is not installed, ignoring host!");
                continue;
            }

            // Is scapy installed?
            sshReturn = ssh.sendCommand("scapy -h");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": scapy is not installed, ignoring host!");
                continue;
            }

            // Is the LLDP script there?
            sshReturn = ssh.sendCommand("[ -f ~/usr/bin/send-lldp ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": the LLDP code is not installed, ignoring host!");
                continue;
            }

            // Checking that the base-box is there
            sshReturn = ssh.sendCommand("[ $(virsh list --all | grep base-box_default | wc -l) -eq 1 ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": the base-box is not there, ignoring host!");
                continue;
            }

            // Delete any remaining VMs on the host and stop DPDK
            boolean ignoreHost = false;
            for(int vmId = 0; vmId <= vmsPerServer; vmId++) {
                sshReturn = ssh.sendCommand("delete-vm " + vmId);
                if(sshReturn.getExitStatus() != 0) {
                    logger.error(expectedHost.getAddress() + ": could not ensure VM " + vmId + " is correctly removed, ignoring host!");
                    ignoreHost = true;
                    break;
                }

                String vmIdString = String.valueOf(vmId);
                if(vmId < 10)
                    vmIdString = "0" + vmIdString;

                ssh.sendCommand("virsh destroy " + vmId + "_" + expectedHost.getAddress().getHostName() + "-vm-" + vmIdString);
                ssh.sendCommand("virsh undefine " + vmId + "_" + expectedHost.getAddress().getHostName() + "-vm-" + vmIdString);
            }

            if(ignoreHost)
                continue;

            // Checking no VM is running
            sshReturn = ssh.sendCommand("[ $(ls /vagrant/ | wc -l) -eq 0 ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": there are some running VMs in /vagrant/, ignoring host!");
                continue;
            }

            // Checking only the base-box is there
            sshReturn = ssh.sendCommand("[ $(virsh list --all | wc -l) -eq 4 ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": either base-box is not there, or something else is there! ignoring host!");
                continue;
            }

            // Checking that the base-box is turned off
            sshReturn = ssh.sendCommand("[ $(virsh list --all | grep base-box_default | grep \"shut off\" | wc -l) -eq 1 ]");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": the base-box is not turned off, ignoring host!");
                continue;
            }

            sshReturn = ssh.sendCommand("stop-dpdk-tagging " + expectedHost.getIfcAddress());
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": could not ensure DPDK is correctly stopped, ignoring host!");
                continue;
            }

            // Everything is fine, add the host
            logger.info(expectedHost.getAddress() + " is all good!");
            hostsWithVerifiedDependencies.add(expectedHost);
        }

        return hostsWithVerifiedDependencies;
    }

    @ComponentStateIs(State = ComponentStatus.New)
    // Synchronized so that topology discover is started correctly (size check)
    public synchronized void newSwitch(DetectedSwitch detectedSwitch) {
        connectedSwitches.put(detectedSwitch.getAddress(), detectedSwitch);
        logger.info("New switch " + detectedSwitch + " - " + connectedSwitches.size() + " switches: " + connectedSwitches);
        NetworkNode node = networkingSystem.createNode(network, detectedSwitch.toString());
        switchToNode.put(detectedSwitch.getAddress(), node);

        try {
            configureFlowRules(detectedSwitch);
        } catch (UnknownHostException e) {
            // is that possible?
            logger.error("That shouldn't be possible because we create 0.0.0.0 IPs...");
        }

        if(connectedSwitches.size() == expectedSwitches.size()) {
            // All the switches are there
            startTopologyDiscovery();

            // When topology is discovered, we can tell the hosts to start DPDK
            startDPDKApplications();

            startupFinished = true;
        }
    }

    public boolean isStartupFinished() {
        return startupFinished;
    }

    private void configureFlowRules(DetectedSwitch detectedSwitch) throws UnknownHostException {
        if(detectedSwitch instanceof OFSwitch) {
            int xid = 6000;

            logger.debug(detectedSwitch + ": deleting all flows on the switch");
            detectedSwitch.getChannel().writeAndFlush(OFMessageFactory.getFactory().createFlowModDeleteAllMessage(xid++));

            for(OFPortStructure portStructure : ((OFSwitch) detectedSwitch).getPorts()) {
                for(int queueId = 0; queueId < switchCapabilities.getNumberOfQueues(detectedSwitch); queueId++) {
                    OFMatchStructure match = OFMatchStructure.getVLANMatch(queueToVlan(portStructure.getId(), queueId));
                    OFActionStructure[] actions = new OFActionStructure[] {
                            new OFActionStripVlan(),
                            new OFActionEnqueue(portStructure.getId(), queueId),
                    };

                    logger.debug(detectedSwitch + ": sending FLOW_MOD: match on VLAN " + queueToVlan(portStructure.getId(), queueId) + ", strip VLAN and output to " + portStructure.getId());
                    detectedSwitch.getChannel().writeAndFlush(OFMessageFactory.getFactory().createFlowModAddMessage(xid++, match, actions, 50));
                }
            }

            // PACKET_IN everything, though we'll only parse LLDP -- just to be aware of everything
            detectedSwitch.getChannel().writeAndFlush(OFMessageFactory.getFactory().createFlowModAddMessage(xid++, OFMatchStructure.getAllMatch(), new OFActionStructure[]{new OFActionOutput(OFActionOutput.CONTROLLER)}, 30));
        }
        else {
            logger.error("Unknown type of switch: " + detectedSwitch);
            // here handle other types of switches
        }
    }

    private int queueToVlan(int portId, int queueId) {
        return queueId + portId * 10;
    }

    private ExpectedHost isExpectedHost(InetAddress address) {
        for(ExpectedHost expectedHost : expectedHosts) {
            if(expectedHost.getAddress().equals(address))
                return expectedHost;
        }

        return null;
    }

    @ComponentStateIs(State = ComponentStatus.New)
    public synchronized void newLink(DetectedLink link) {
        // TODO check if link was there already

        logger.info("New link " + link);

        ExpectedHost expectedHost = isExpectedHost(link.getSource());
        if(expectedHost != null) {
            // It's a host link
            Host host = networkingSystem.createHost(network, expectedHost.getAddress().getCanonicalHostName());
            // Note: we only support hosts with one interface
            NetworkNode ifc = networkingSystem.addInterface(host, new NetworkInterface(expectedHost.getIfcAddress(), "00:00:00:00:00:00"));
            networkingSystem.createLinkWithPriorityScheduling(ifc, switchToNode.get(link.getDestination()), 1e9 / ((double) 8), 0, new double[]{Double.POSITIVE_INFINITY});
            addLinkFromSwitchToNode(connectedSwitches.get(link.getDestination()), link.getDstPortId(), ifc);
            return;
        }

        NetworkNode srcNode = switchToNode.get(link.getSource());
        NetworkNode dstNode = switchToNode.get(link.getDestination());

        if(srcNode == null)
            logger.error("Source node of new link is unknown");
        else if(dstNode == null)
            logger.error("Destination node of new link is unknown");
        else {
            addLinkFromSwitchToNode(connectedSwitches.get(link.getSource()), link.getSrcPortId(), dstNode);
        }
    }

    private void addLinkFromSwitchToNode(DetectedSwitch detectedSwitch, int sourcePortId, NetworkNode dstNode) {
        try {
            int nQueues = switchCapabilities.getNumberOfQueues(detectedSwitch);
            int maxPorts = 4;
            if(detectedSwitch.getAddress().getCanonicalHostName().equals("tottenham.forschung.lkn.ei.tum.de") || detectedSwitch.getAddress().getCanonicalHostName().equals("koeln.forschung.lkn.ei.tum.de"))
                maxPorts = 2;
            if(detectedSwitch.getAddress().getCanonicalHostName().equals("newcastle.forschung.lkn.ei.tum.de")
                    || detectedSwitch.getAddress().getCanonicalHostName().equals("watford.forschung.lkn.ei.tum.de")
                    || detectedSwitch.getAddress().getCanonicalHostName().equals("westham.forschung.lkn.ei.tum.de")
                    || detectedSwitch.getAddress().getCanonicalHostName().equals("fulham.forschung.lkn.ei.tum.de"))
                maxPorts = 3;
            double bufferSize = switchCapabilities.getPerQueueBufferSize(detectedSwitch, maxPorts);
            double[] bufferSizes = new double[nQueues];
            for(int i = 0; i < bufferSizes.length; i++)
                bufferSizes[i] = bufferSize;

            double processingTime = switchCapabilities.getProcessingTime(detectedSwitch);
            double queuingOverhead = switchCapabilities.getPriorityQueuingOverhead(detectedSwitch);

            Link link = networkingSystem.createLinkWithPriorityScheduling(switchToNode.get(detectedSwitch.getAddress()), dstNode, 1e9 / ((double) 8), (processingTime + queuingOverhead) / 1e6, bufferSizes);
            linksToSourcePortId.put(link, sourcePortId);
        } catch (UnknownHostException e) {
            logger.error("Source node " + detectedSwitch + " unknown to the switch capabilities class");
        }
    }

    @ComponentStateIs(State = ComponentStatus.Destroyed)
    public void switchGone(DetectedSwitch detectedSwitch) {
        connectedSwitches.remove(detectedSwitch.getAddress());
        logger.info(detectedSwitch + " gone - switches: " + connectedSwitches);
        switchToNode.remove(detectedSwitch.getAddress());
    }

    private void startDPDKApplications() {
        logger.info("Starting DPDK applications on the hosts");

        for(ExpectedHost expectedHost : expectedHosts) {
            // SSH and start DPDK
            SSHManager ssh;
            try {
                ssh = new SSHManager("root", expectedHost.getAddress().getCanonicalHostName());
            } catch (JSchException e) {
                logger.error("Impossible to SSH to " + expectedHost.getAddress() + ": " + e);
                continue;
            }

            SSHReturn sshReturn;

            // Build and start the DPDK app
            sshReturn = ssh.sendCommand("start-dpdk-tagging " + expectedHost.getIfcAddress());
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": unable to start DPDK application: " + sshReturn.getOutput() + "\t" + sshReturn.getErrOutput());
                continue;
            }

            logger.info(expectedHost.getAddress() + ": DPDK started!");

            ssh.close();
        }
    }

    private void startTopologyDiscovery() {
        logger.info("Starting topology discovery");

        // Tell the switches to send LLDP packets
        for(Map.Entry<InetAddress, DetectedSwitch> detectedSwitchEntry : connectedSwitches.entrySet()) {
            DetectedSwitch detectedSwitch = detectedSwitchEntry.getValue();
            if(detectedSwitch instanceof OFSwitch) {
                OFSwitch ofSwitch = (OFSwitch) detectedSwitch;
                int xid = 1000;
                for (OFPortStructure port : ofSwitch.getPorts()) {
                    OFActionStructure action = new OFActionOutput(port.getId());
                    byte[] packet = LLDPUtils.generateTopologyDiscoveryPacket(ofSwitch.getAddress(), port.getMacAddress(), port.getId());
                    OFPacketOutMessage packetOut = OFMessageFactory.getFactory().createPacketOutMessage(xid++, 0xffffffff, 0x0000, new OFActionStructure[]{action}, packet);
                    ofSwitch.getChannel().writeAndFlush(packetOut);
                }
            }
            else {
                // Here handle other types of switches
                logger.error("Unknown type of switch: " + detectedSwitch);
            }
        }

        // Tell the hosts to send LLDP packets
        for(ExpectedHost expectedHost : expectedHosts) {
            // SSH and send LLDP using scapy
            SSHManager ssh;
            try {
                ssh = new SSHManager("root", expectedHost.getAddress().getCanonicalHostName());
            } catch (JSchException e) {
                logger.error("Impossible to SSH to " + expectedHost.getAddress() + ": " + e);
                continue;
            }

            SSHReturn sshReturn;

            // send LLDP
            sshReturn = ssh.sendCommand("/usr/bin/send-lldp " + expectedHost.getAddress().getHostAddress() + " $(ls -l /sys/class/net/ | grep " + expectedHost.getIfcAddress() + " | rev | cut -d \"/\" -f 1 | rev)");
            if(sshReturn.getExitStatus() != 0) {
                logger.error(expectedHost.getAddress() + ": unable to send LLDP packet: " + sshReturn.getOutput() + "\t" + sshReturn.getErrOutput());
                continue;
            }

            logger.info(expectedHost.getAddress() + ": LLDP packet sent!");

            ssh.close();
        }
    }

    private synchronized void configureEndHost(Flow flow, Path path, RuleId ruleId) {
        String sourceHostname = flow.getSource().getHostMachine().getName();

        // Computing tags
        StringBuilder tagsString = new StringBuilder();
        for(int edgeId = 1; edgeId < path.getPath().length; edgeId++) { // first edge does not matter as it's from the server
            Edge queueEdge = path.getPath()[edgeId];

            // Link-level: find port ID
            int portId = linksToSourcePortId.get(linkMapper.get(toNetworkMapper.get(queueEdge.getEntity()).getNetworkEntity()));
            // Queue-level: find queue ID
            int queueId = 0;
            for(Edge outEdgesOfSourceNode : linkMapper.get(toNetworkMapper.get(queueEdge.getEntity()).getNetworkEntity()).getQueueEdges()) {
                if(outEdgesOfSourceNode == queueEdge)
                    break;
                else
                    queueId++;
            }

            int vlanId = queueToVlan(portId, queueId);
            tagsString.append(vlanId).append(",");
        }

        // Last tag is the ID of the destination VM
        tagsString.append(vmIdMapper.get(flow.getDestination().getEntity()).getIntegerId());

        // SSH and create the rule on the source host
        SSHManager ssh;
        try {
            ssh = new SSHManager("root", sourceHostname, CONTROL_VM_PORT);
            logger.info("Updating matching on " + sourceHostname + " for " + flow);
            String command = "update-matching-table "
                    + vmIdMapper.get(flow.getSource().getEntity()).getIntegerId() + " "
                    + ruleId.getIntegerId() + " "
                    + ((FiveTupleMatching) flow.getMatching()).getProtocol() + " "
                    + ((FiveTupleMatching) flow.getMatching()).getSourceIP().getHostAddress() + " "
                    + ((FiveTupleMatching) flow.getMatching()).getDestinationIP().getHostAddress() + " "
                    + ((FiveTupleMatching) flow.getMatching()).getSourcePort() + " "
                    + ((FiveTupleMatching) flow.getMatching()).getDestinationPort() + " "
                    + tagsString + " "
                    + ((TokenBucketTrafficContract) flow.getTrafficContract()).getRate() + " "
                    + ((TokenBucketTrafficContract) flow.getTrafficContract()).getBurst() * 8; // we have in bytes and command wants bits
            logger.info(sourceHostname + " " + command);
            SSHReturn sshReturn = ssh.sendCommand(command);
            if(sshReturn.getExitStatus() != 0) {
                logger.error(sourceHostname + ": unable to update matching table entry: " + sshReturn.getOutput() + "\t" + sshReturn.getErrOutput());
                return;
            }
        } catch (JSchException e) {
            logger.error("Impossible to SSH to " + sourceHostname + ": " + e);
            return;
        }

        ssh.close();
    }

    @ComponentStateIs(State = ComponentStatus.New)
    @HasComponent(component = Path.class)
    // synchronized for correct rule ID track keeping
    public synchronized void newFlow(Flow flow) {
        String sourceHostname = flow.getSource().getHostMachine().getName();

        RuleId ruleId;
        if(!rerouteFromMapper.isIn(flow.getEntity())) {
            // Get rule ID
            RuleIdList ruleIdsListComponent = ruleIdListMapper.get(flow.getSource().getEntity());
            List<RuleId> availableRuleIds = ruleIdsListComponent.getAvailableIds();
            if (availableRuleIds.size() == 0) {
                logger.error("I was supposed to embed flow " + flow + " on " + sourceHostname + " but there are no more available rule IDs");
                return;
            }
            ruleId = availableRuleIds.get(0);
        }
        else {
            // It's a reroute
            ruleId = ruleIdMapper.get(rerouteFromMapper.get(flow.getEntity()).getPath());
        }

        // Getting five-tuple elements of the matching
        Matching matching = flow.getMatching();
        if(!(matching instanceof FiveTupleMatching)) {
            logger.error("We only support five-tuple matching! Cannot embed flow!!!");
            return;
        }
        InetAddress sourceIp = ((FiveTupleMatching) matching).getSourceIP();
        InetAddress destinationIp = ((FiveTupleMatching) matching).getDestinationIP();
        if(!(sourceIp instanceof Inet4Address && destinationIp instanceof Inet4Address)) {
            logger.error("We only support IPv4 addresses! Cannot embed flow!!!");
            return;
        }
        int sourcePort = ((FiveTupleMatching) matching).getSourcePort();
        int destinationPort = ((FiveTupleMatching) matching).getDestinationPort();
        int protocol = ((FiveTupleMatching) matching).getProtocol();

        // Getting traffic contract (TB)
        TrafficContract trafficContract = flow.getTrafficContract();
        if(!(trafficContract instanceof TokenBucketTrafficContract)) {
            logger.error("We only support token-bucket traffic contracts! Cannot embed flow!!!");
            return;
        }

        // Getting path of the flow
        Path path = pathMapper.get(flow.getEntity());

        logger.info("Supposed to embed " + path + ": " + sourceIp + ":" + sourcePort + "->" + destinationIp + ":" + destinationPort + " (" + protocol + ") [R:" + ((TokenBucketTrafficContract) trafficContract).getRate() + "|B:" + ((TokenBucketTrafficContract) trafficContract).getBurst() + "]");
        configureEndHost(flow, path, ruleId);

        if(rerouteFromMapper.isIn(flow.getEntity())) {
            // That was a reroute
            logger.info("Done: matching updated on " + sourceHostname + " for " + flow + " (REROUTING)");
        }
        else {
            // That was a new flow!
            RuleIdList ruleIdsListComponent = ruleIdListMapper.get(flow.getSource().getEntity());
            ruleIdMapper.attachComponent(flow.getEntity(), ruleId);
            ruleIdListMapper.updateComponent(ruleIdsListComponent, () -> ruleIdsListComponent.removeId(ruleId));
            logger.info("Done: matching updated on " + sourceHostname + " for " + flow);
        }
    }

    @ComponentStateIs(State = ComponentStatus.New)
    public void newHost(Host host) {
        // Add an initial list of available VMs for the host
        vmIdListMapper.attachComponent(host.getEntity(), new VMIdList(vmsPerServer));
    }

    @ComponentStateIs(State = ComponentStatus.New)
    // Synchronized so that IDs are computed correctly
    public synchronized void newVM(VirtualMachine vm) {
        // We created hosts so we know the host object name is the real hostname
        Host host = vm.getHostMachine();
        String hostname = host.getName();
        VMIdList vmIdListComponent = vmIdListMapper.get(vm.getHostMachine().getEntity());
        List<VMId> availableVmIds = vmIdListComponent.getAvailableIds();
        if(availableVmIds.size() == 0) {
            logger.error("I was supposed to create a VM on " + hostname + " but there are no more available VM IDs");
            return;
        }

        // Get a random (the first) VM ID.
        VMId newVmId = availableVmIds.get(0);

        // SSH and create the VM
        String postCommand = "";
        String preCommand = "";
        if(!this.waitForVmCreation) {
            preCommand = "sh -c 'nohup ";
            postCommand = " > /dev/null 2>&1 & '";
        }

        SSHManager ssh;
        try {
            ssh = new SSHManager("root", hostname);

            if(!controlVmCreated.contains(hostname)) {
                logger.info("Creating control VM on " + hostname);
                SSHReturn sshReturn = ssh.sendCommand(preCommand + "create-vm 0" + postCommand);
                if(sshReturn.getExitStatus() != 0) {
                    logger.error(hostname + ": unable to create control VM: " + sshReturn.getOutput() + "\t" + sshReturn.getErrOutput());
                    return;
                }

                controlVmCreated.add(hostname);
            }

            logger.info("Creating VM " + vm + " on " + hostname);
            SSHReturn sshReturn = ssh.sendCommand(preCommand + "create-vm " + newVmId.getIntegerId() + postCommand);
            if(sshReturn.getExitStatus() != 0) {
                logger.error(hostname + ": unable to create VM: " + sshReturn.getOutput() + "\t" + sshReturn.getErrOutput());
                return;
            }
        } catch (JSchException e) {
            logger.error("Impossible to SSH to " + hostname + ": " + e);
            return;
        }
        ssh.close();

        virtualMachineMapper.updateComponent(vm, () -> vm.setManagementConnection("ssh -p " + (20000 + newVmId.getIntegerId()) + " root@" + hostname));
        vmIdListMapper.updateComponent(vmIdListComponent, () -> vmIdListComponent.removeId(newVmId));
        vmIdMapper.attachComponent(vm.getEntity(), newVmId);
        // Add an initial list of available rules IDs for the VM
        ruleIdListMapper.attachComponent(vm.getEntity(), new RuleIdList(rulesPerVm));
        logger.info(hostname + ": VM " + vm + " created on " + hostname + " with id " + newVmId.getIntegerId());

    }
}
