package de.tum.ei.lkn.eces.sbi;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.DNMSystem;
import de.tum.ei.lkn.eces.dnm.ResidualMode;
import de.tum.ei.lkn.eces.dnm.config.ACModel;
import de.tum.ei.lkn.eces.dnm.config.BurstIncreaseModel;
import de.tum.ei.lkn.eces.dnm.config.DetServConfig;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Division;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.LowerLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Summation;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.UpperLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.Constant;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.QueuePriority;
import de.tum.ei.lkn.eces.dnm.mappers.DetServConfigMapper;
import de.tum.ei.lkn.eces.dnm.proxies.DetServProxy;
import de.tum.ei.lkn.eces.dnm.resourcemanagement.resourceallocation.MHM.MHMRateRatiosAllocation;
import de.tum.ei.lkn.eces.graph.GraphSystem;
import de.tum.ei.lkn.eces.network.Network;
import de.tum.ei.lkn.eces.network.NetworkingSystem;
import de.tum.ei.lkn.eces.network.color.DelayColoring;
import de.tum.ei.lkn.eces.network.color.QueueColoring;
import de.tum.ei.lkn.eces.network.color.RateColoring;
import de.tum.ei.lkn.eces.routing.RoutingSystem;
import de.tum.ei.lkn.eces.routing.algorithms.RoutingAlgorithm;
import de.tum.ei.lkn.eces.routing.algorithms.csp.unicast.cbf.CBFAlgorithm;
import de.tum.ei.lkn.eces.webgraphgui.WebGraphGuiSystem;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringSystem;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("de.tum.ei.lkn.eces.dnm").setLevel(Level.ERROR);
        Logger.getLogger("de.tum.ei.lkn.eces.graph").setLevel(Level.ERROR);
        Logger.getLogger("de.tum.ei.lkn.eces.network").setLevel(Level.ERROR);

        Controller controller = new Controller();
        GraphSystem graphSystem = new GraphSystem(controller);
        NetworkingSystem networkingSystem = new NetworkingSystem(controller, graphSystem);

        DetServConfig modelConfig = new DetServConfig(
                ACModel.MHM,
                ResidualMode.LEAST_LATENCY,
                BurstIncreaseModel.NO,
                false,
                new LowerLimit(new UpperLimit(
                        new Division(new Constant(), new Summation(new Constant(), new QueuePriority())),
                        1), 0),
                (controller1, scheduler) -> new MHMRateRatiosAllocation(controller1, new double[]{1.0/4, 1.0/5, 1.0/6, 1.0/8, 0, 0, 0, 0}));


        // DNC
        new DNMSystem(controller);
        DetServProxy proxy = new DetServProxy(controller);

        // Routing
        new RoutingSystem(controller);
        RoutingAlgorithm cbf = new CBFAlgorithm(controller);
        cbf.setProxy(proxy);
        modelConfig.initCostModel(controller);

        // Create network
        Network network = networkingSystem.createNetwork();
        DetServConfigMapper modelingConfigMapper = new DetServConfigMapper(controller);
        modelingConfigMapper.attachComponent(network.getQueueGraph(), modelConfig);

        // GUI
        ColoringSystem myColoringSys = new ColoringSystem(controller);
        myColoringSys.addColoringScheme(new DelayColoring(controller), "Delay");
        myColoringSys.addColoringScheme(new QueueColoring(controller), "Queue sizes");
        myColoringSys.addColoringScheme(new RateColoring(controller), "Link rate");
        new WebGraphGuiSystem(controller, myColoringSys, 8080);

        // SBI
        Set<ExpectedHost> hosts = new HashSet<>();
        hosts.add(new ExpectedHost(InetAddress.getByName("scholes.forschung.lkn.ei.tum.de"), "0000:04.00.0"));
        hosts.add(new ExpectedHost(InetAddress.getByName("kane.forschung.lkn.ei.tum.de"), "0000:04.00.0"));
        hosts.add(new ExpectedHost(InetAddress.getByName("gerrard.forschung.lkn.ei.tum.de"), "0000:04.00.0"));

        Set<InetAddress> switches = new HashSet<>();
        switches.add(InetAddress.getByName("leicester.forschung.lkn.ei.tum.de"));
        //switches.add(InetAddress.getByName("leeds.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("watford.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("newcastle.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("tottenham.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("liverpool.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("mancity.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("westham.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("fulham.forschung.lkn.ei.tum.de"));
        switches.add(InetAddress.getByName("chelsea.forschung.lkn.ei.tum.de"));

        new SBISystem(controller, networkingSystem, network, hosts, switches, 10, 3, false);
    }
}
