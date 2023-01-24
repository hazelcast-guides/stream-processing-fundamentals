package hazelcast.platform.solutions.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.map.IMap;
import hazelcast.platform.solutions.machineshop.domain.MachineProfile;
import hazelcast.platform.solutions.machineshop.domain.MachineStatusEvent;
import hazelcast.platform.solutions.machineshop.domain.Names;

import java.util.Map;

public class TemperatureMonitorPipeline {

    private static String categorizeTemp(double temp, short warningLimit, short criticalLimit){
        String result;
        if (temp > (double) criticalLimit)
            result = "red";
        else if (temp > (double) warningLimit)
            result = "orange";
        else
            result = "green";

        return result;
    }

    public static Pipeline createPipeline(){
        Pipeline pipeline = Pipeline.create();

        // create a stream of MachineStatusEvent events from the map journal, use the timestamps embedded in the events
        StreamStage<Map.Entry<String, MachineStatusEvent>> statusEvents = pipeline.readFrom(
                        Sources.<String, MachineStatusEvent>mapJournal(
                                Names.EVENT_MAP_NAME,
                                JournalInitialPosition.START_FROM_OLDEST))
                .withTimestamps(item -> item.getValue().getEventTime(), 1000)
                .setName("machine status events");

        // split the events by serial number, create a tumbling window to calculate avg. temp over 10s
        // output is a tuple: serial number, avg temp
        StreamStage<KeyedWindowResult<String, Double>> averageTemps = statusEvents
                .groupingKey( entry -> entry.getValue().getSerialNum())
                .window(WindowDefinition.tumbling(10000))
                .aggregate(AggregateOperations.averagingLong( item -> item.getValue().getBitTemp()))
                .setName("Average Temp");

        // look up the machine profile for this machine, copy the warning temp onto the event
        // the output is serial number, avg temp, warning temp, critical temp
        StreamStage<Tuple4<String, Double, Short, Short>> temperaturesAndLimits =
                averageTemps.groupingKey(KeyedWindowResult::getKey).
                <MachineProfile, Tuple4<String, Double, Short, Short>>mapUsingIMap(Names.PROFILE_MAP_NAME,
                (window, machineProfile) -> Tuple4.tuple4(window.getKey(), window.getValue(), machineProfile.getWarningTemp(), machineProfile.getCriticalTemp()))
                .setName("Lookup Temp Limits");

        // categorize as GREEN / ORANGE / RED, output is serial number, category
        StreamStage<Tuple2<String,String>> labels =
                temperaturesAndLimits.map(tuple -> Tuple2.tuple2(tuple.f0(), categorizeTemp(tuple.f1(), tuple.f2(), tuple.f3())))
                .setName("Apply Label");

        // some thoughts on why this works and when it wouldn't
        // this only works if this pipeline is the only updater of the controls map
        ServiceFactory<?, IMap<String, String>> iMapServiceFactory =
                ServiceFactories.iMapService(Names.CONTROLS_MAP_NAME);

        labels.groupingKey(Tuple2::f0)
                .mapUsingService(iMapServiceFactory,
                        (map, key, item) -> {
                    String currStatus = map.get(key);
                    if (!item.f1().equals(currStatus)) {
                        map.put(key, item.f1());
                        return item;
                    } else {
                        return Tuple2.tuple2(key, "NOOP");
                    }
                })
                .setName("Save status changes")
                .writeTo(Sinks.noop());


        return pipeline;
    }
    public static void main(String []args){
        Pipeline pipeline = createPipeline();
        pipeline.setPreserveOrder(true);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Temperature Monitor");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
