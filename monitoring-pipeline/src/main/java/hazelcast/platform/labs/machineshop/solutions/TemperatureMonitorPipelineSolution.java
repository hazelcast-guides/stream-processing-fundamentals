package hazelcast.platform.labs.machineshop.solutions;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import hazelcast.platform.labs.machineshop.domain.Names;

import java.io.Serializable;
import java.util.Map;

/*
 **************** SOLUTION ****************
 */

public class TemperatureMonitorPipelineSolution {

    /*
     * Used for stateful filtering.  Must be serializable as it is included in stream snapshot
     */
    public static class CurrentState implements Serializable {
        private String color;

        public CurrentState(){
            color="";
        }

        public String getColor(){
            return color;
        }

        public void setColor(String color){
            this.color = color;
        }

    }

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

    /*
     * Write your Pipeline here.
     *
     * DataStructureDefinitions
     *
     *   GenericRecord of MachineStatusEvent;
     *     GenericRecord machineEvent;
     *     String serialNum = machineEvent.getString("serialNum);
     *     long eventTime = machineEvent.getInt64("eventTime");
     *     short bitTemp = machineEvent.getInt16("bitTemp");
     *
     *   GenericRecord of MachineProfile
     *     GenericRecord profile;
     *     String serialNum = profile.getString("serialNum");
     *     String location = profile.getString("location");
     *     String block = profile.getString("block");
     *     short warningTemp = profile.getInt16("warningTemp");
     *     short criticalTemp = profile.getInt16("criticalTemp");
     *
     * Useful References:
     *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStage.html
     */
    public static Pipeline createPipeline(){
        Pipeline pipeline = Pipeline.create();

        /*
         * Read events from the "machine_events" map.  The key is serialNumber and the value is a GenericRecord
         * containing a MachineStatusEvent.
         */
        StreamStage<Map.Entry<String, GenericRecord>> statusEvents = pipeline.readFrom(
                        Sources.<String, GenericRecord>mapJournal(
                                Names.EVENT_MAP_NAME,
                                JournalInitialPosition.START_FROM_OLDEST))
                .withTimestamps(item -> item.getValue().getInt64("eventTime"), 1000)
                .setName("machine status events");

        /*
         * Group the events by serial number. For each serial number, compute the average temperature over a 10s
         * tumbling window.
         *
         * The general template for aggregation looks like this:
         *
         * StreamStage<KeyedWindowResult<String, Double>> averageTemps = statusEvents.groupingKey( GET KEY LAMBDA )
         *                                  .window( WINDOW DEFINITION )
         *                                  .aggregate(AggregateOperations.averagingLong( GET BIT TEMP LAMBDA);
         *
         * See:
         *   https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/WindowDefinition.html
         *   https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/aggregate/AggregateOperations.html
         *
         */
        StreamStage<KeyedWindowResult<String, Double>> averageTemps = statusEvents
                .groupingKey( entry -> entry.getValue().getString("serialNum"))
                .window(WindowDefinition.tumbling(10000))
                .aggregate(AggregateOperations.averagingLong( item -> item.getValue().getInt16("bitTemp")))
                .setName("Average Temp").peek();

        /*
         * Look up the machine profile for this machine from the machine_profiles map.  Output a
         * 4-tuple (serialNum, avg temp, warning temp, critical_temp)
         *
         * Note that the incoming event already has a key associated with it (serial number).  mapUsingImap
         * on a StreamStageWithKey will automatically use they key of the event to do the lookup on the map.
         * The general form is:
         *
         * StreamStage<Tuple4<String,Double,Short,Short>> temperaturesAndLimits
         *      = averageTemps.mapUsingIMap( (w, p) -> LAMBDA RETURNING Tuple4)
         *
         * where p is a MachineProfile GenericRecord
         *       w is the KeyedWindowResult from the previous stage.
         *
         * See:
         *   "mapUsingIMap" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStageWithKey.html
         *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/datamodel/KeyedWindowResult.html
         *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/datamodel/Tuple4.html
         */
        StreamStage<Tuple4<String, Double, Short, Short>> temperaturesAndLimits =
                averageTemps.groupingKey(KeyedWindowResult::getKey)
                        .<GenericRecord, Tuple4<String, Double, Short, Short>>mapUsingIMap(Names.PROFILE_MAP_NAME,
                (window, mp) -> Tuple4.tuple4(window.getKey(), window.getValue(), mp.getInt16("warningTemp"), mp.getInt16("criticalTemp")))
                .setName("Lookup Temp Limits");

        /*
         * Using a simple "map" stage, categorize the temperature as "green", "red" or "orange" and
         * return a Tuple2 (serialNum, color).
         *
         * See:
         *   the "categorizeTemp" function at the top of this file
         *   "map" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStage.html
         */
        StreamStage<Tuple2<String,String>> labels =
                temperaturesAndLimits.map(tuple -> Tuple2.tuple2(tuple.f0(), categorizeTemp(tuple.f1(), tuple.f2(), tuple.f3())))
                .setName("Apply Label");

        /*
         * We only want to write to the output map if the current color has changed.  This prevents flooding the
         * map listeners with irrelevant events.  We can use   StreamStageWithKey.filterStateful to do this.
         * The filter will remember the last value for each key.
         *
         * The CurrentState class in this file should be used to hold the remembered value.
         *
         * The solution will look like this:
         *
         * StreamStage<Tuple2<String,String>> changedLabels =
         *   labels.groupingKey(GET SERIAL NUM LAMBDA)
         *         .filterStateful(CurrentState::new, (cs, event) -> FILTER LAMBDA)
         *
         * Where cs is the instance of current state related to this key
         *       event is the Tuple2 event from the previous stage.
         *
         * Note:
         *    When the incoming value is not equal to the previous value, don't forget to update the CurrentState
         *    object with the new value
         *
         * See:
         *    "filterStateful" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStageWithKey.html
         */
        StreamStage<Tuple2<String,String>> changedLabels =
                labels.groupingKey(Tuple2::f0)
                        .filterStateful(CurrentState::new,
                                (cs, label) -> {
                                    boolean same = cs.getColor().equals(label.f1());
                                    if (!same) cs.setColor(label.f1());
                                    return !same;
                                }).setName("Label Changes");

        /*
         * Finally, we can sink the results directly to the "machine_controls" map.  Tuple2<K,V> also implements
         * Map.Entry<K,V> so we can just supply it directly to the IMap Sink.
         *
         * Create a Sink using Sinks.map (see reference) then finish the pipeline with
         * changedLabels.writeTo(machineControlsSink);
         *
         * See:
         *    "map" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/Sinks.html
         */
        Sink<Map.Entry<String,String>> sink = Sinks.map(Names.CONTROLS_MAP_NAME);
        changedLabels.writeTo(sink);

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
