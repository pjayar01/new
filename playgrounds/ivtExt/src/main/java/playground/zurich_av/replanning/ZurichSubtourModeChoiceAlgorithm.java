package playground.zurich_av.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.Collection;

public class ZurichSubtourModeChoiceAlgorithm implements PlanAlgorithm {
    final private Network network;
    final private StageActivityTypes stageActivityTypes;
    final private Collection<Id<Link>> permissibleLinkIds;

    final private PlanAlgorithm choiceAlgorithmWithoutAV;
    final private PlanAlgorithm choiceAlgorithmWithAV;

    private Counter rejectionCounter = new Counter("Rejected AV Plans ");

    public ZurichSubtourModeChoiceAlgorithm(Network network, StageActivityTypes stageActivityTypes, PlanAlgorithm choiceAlgorithmWithAV, PlanAlgorithm choiceAlgorithmWithoutAV, Collection<Id<Link>> permissibleLinkIds) {
        this.network = network;
        this.stageActivityTypes = stageActivityTypes;
        this.choiceAlgorithmWithAV = choiceAlgorithmWithAV;
        this.choiceAlgorithmWithoutAV = choiceAlgorithmWithoutAV;
        this.permissibleLinkIds = permissibleLinkIds;
    }

    @Override
    public void run(Plan plan) {
        Plan copy = PlanUtils.createCopy(plan);
        choiceAlgorithmWithAV.run(copy);

        if (isPlanPermissible(copy)) {
            PlanUtils.copyFrom(copy, plan);
        } else {
            rejectionCounter.incCounter();
            choiceAlgorithmWithoutAV.run(plan);
        }
    }

    boolean isPlanPermissible(Plan plan) {
        for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan.getPlanElements(), stageActivityTypes)) {
            for (Leg leg : trip.getLegsOnly()) {
                if (!leg.getMode().equals(AVModule.AV_MODE)) continue;

                Link origin = network.getLinks().get(trip.getOriginActivity().getLinkId());
                Link destination = network.getLinks().get(trip.getDestinationActivity().getLinkId());

                if (!permissibleLinkIds.contains(origin.getId()) || !permissibleLinkIds.contains(destination.getId())) {
                    return false;
                }
            }
        }

        return true;
    }
}
