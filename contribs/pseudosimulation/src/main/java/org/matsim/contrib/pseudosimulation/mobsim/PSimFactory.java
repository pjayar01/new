/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.Collection;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;

import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;

/**
 * @author fouriep
 */
public class PSimFactory implements Provider<Mobsim>, MobsimFactory {

    private Collection<Plan> plans;
    private TravelTime travelTime;
    private WaitTime waitTime;
    private StopStopTime stopStopTime;
    private TransitPerformance transitPerformance;
    private final Scenario scenario;
    private final EventsManager eventsManager;

    public PSimFactory(Scenario scenario, EventsManager eventsManager) {
        this.scenario = scenario;
        this.eventsManager = eventsManager;
    }

    @Override
    public Mobsim get() {
//		if (iteration > 0)
//			eventsManager.resetHandlers(iteration++);
//		else
//			iteration++;
        if (waitTime != null) {
            return new PSim(scenario, eventsManager, plans, travelTime, waitTime, stopStopTime, transitPerformance);

        } else {
            return new PSim(scenario, eventsManager, plans, travelTime);
        }
    }

    public void setPlans(Collection<Plan> plans) {
        this.plans = plans;
    }

    public void setTravelTime(TravelTime travelTime) {
        this.travelTime = travelTime;
    }

    public void setWaitTime(WaitTime waitTime) {
        this.waitTime = waitTime;
    }

    public void setStopStopTime(StopStopTime stopStopTime) {
        this.stopStopTime = stopStopTime;
    }

    public void setTransitPerformance(TransitPerformance transitPerformance) {
        this.transitPerformance = transitPerformance;
    }

    public void setTimes(TravelTime travelTime, WaitTime waitTime, StopStopTime stopStopTime) {
        this.travelTime = travelTime;
        this.waitTime = waitTime;
        this.stopStopTime = stopStopTime;
    }

    @Deprecated
    //will replace where necessary

    public Mobsim createMobsim(Scenario scenario, EventsManager events) {
        return get();
    }
}
