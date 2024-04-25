package hazelcast.platform.labs.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import hazelcast.platform.labs.machineshop.domain.Names;

import java.io.Serializable;
import java.util.Map;

public class TemperatureMonitorPipeline {

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
     *   GenericRecord of MachineEvent;
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
         * containing a MachineEvent.
         */
        StreamStage<Map.Entry<String, GenericRecord>> statusEvents = pipeline.readFrom(
                        Sources.<String, GenericRecord>mapJournal(
                                "PLACEHOLDER",
                                JournalInitialPosition.START_FROM_OLDEST))
                .withTimestamps(item -> item.getValue().getInt64("eventTime"), 1000)
                .setName("machine status events");

        /*
         * At any time, you can add a logging sink to a stream stage to examine the contents.  Remove it when you
         * are done testing.
         */
        statusEvents.writeTo(Sinks.logger( event -> "New Event SN=" + event.getValue().getString("serialNum")));

        /*
         * Group the events by serial number. For each serial number, compute the average temperature over a 10s
         * tumbling window.
         *
         * INPUT: Map.Entry<String, GenericRecord>
         *        The GenericRecord is a MachineEvent. For the specific field names, see the comment
         *        at the top of this class.
         *
         * OUTPUT: KeyedWindowResult<String, Double>
         *
         * The general template for aggregation looks like this:
         *
         * StreamStage<KeyedWindowResult<String, Double>> averageTemps = statusEvents.groupingKey( GET KEY LAMBDA )
         *                                  .window( WINDOW DEFINITION )
         *                                  .aggregate(AggregateOperations.averagingLong( GET BIT TEMP LAMBDA);
         *
         * For available Window Definitions and Aggregations, see:
         *   https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/WindowDefinition.html
         *   https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/aggregate/AggregateOperations.html
         *
         */
        StreamStage<KeyedWindowResult<String, Double>> averageTemps = null;

        /*
         * Look up the machine profile for this machine from the machine_profiles map.  Output a
         * 4-tuple (serialNum, avg temp, warning temp, critical_temp)
         *
         * INPUT: StreamStage<KeyedWindowResult<String, Double>>
         *        streamStage.getKey() is the serial number
         *        streamStage.getValue() is the averageTemperature over the window.
         *
         * OUTPUT: Tuple4<String, Double, Short, Short>
         *         The members of the Tuple4 are: serial number, average temp, warning temp, critical temp)
         *         The last 2 values are looked up from the machine_profiles map using the mapUsingIMap method.
         *
         * We would like for the map lookup to be local which means each event needs to be routed to the
         * machine that owns that machine_profile entry.  This is accomplished by setting a grouping key
         * using the groupingKey method.  The groupingKey method returns a  StreamStageWithKey.  Since the
         * key is already known, StreamStageWithKey.mapUsingImap will automatically use it to do the lookup on the map.
         * As opposed to StreamStage.mapUsingIMap, you do not need to supply a "getKey" function.  Instead, you supply
         * a BiFunction which takes the input even and the value returned from the map lookup and returns a new event.
         *
         * In this case, the value in the machine_profiles map is a GenericRecord of a MachineProfile.  For
         * the available field names, see the comment at the top of this class.
         *
         * The general form is:
         *
         * StreamStage<Tuple4<String,Double,Short,Short>> temperaturesAndLimits
         *      = averageTemps
         *           .groupingKey( GET KEY LAMBDA)
         *           ,mapUsingIMap( Names.PROFILE_MAP_NAME, (w, p) -> LAMBDA RETURNING Tuple4)
         *
         * where p is a MachineProfile GenericRecord
         *       w is the KeyedWindowResult from the previous stage.
         *
         * See:
         *   "mapUsingIMap" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStageWithKey.html
         *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/datamodel/KeyedWindowResult.html
         *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/datamodel/Tuple4.html
         */
        StreamStage<Tuple4<String,Double,Short,Short>> temperaturesAndLimits = null;

        /*
         * Using a simple "map" stage, categorize the temperature as "green", "red" or "orange" and
         * return a Tuple2 (serialNum, color).
         *
         * INPUT: Tuple4<String,Double,Short,Short) i.e.  (serialNum, avg temp, warning temp, critical_temp)
         * OUTPUT: Tuple2<String,String> i.e. (serialNumber, red/orange/green)
         *
         * See:
         *   the "categorizeTemp" function at the top of this class
         *   "map" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStage.html
         */
        StreamStage<Tuple2<String,String>> labels = null;

        /*
         * We  want to write to the output map only if the current color has changed.  This prevents flooding the
         * map listeners with irrelevant events.  We can use   StreamStageWithKey.filterStateful to do this.
         * The filter will remember the last value for each key.
         *
         * INPUT: Tuple2<String,String>  i.e. (serialNumber, red/orange/green)
         * OUTPUT: Tuple2<String,String>  i.e. (serialNumber, red/orange/green)
         *         OR nothing if there is no change relative to the previous event with the same serial number
         *
         * The CurrentState class in this file should be used to hold the remembered value.
         *
         * The solution will look like this:
         *
         * StreamStage<Tuple2<String,String>> changedLabels =
         *   labels.groupingKey(GET SERIAL NUM LAMBDA)
         *         .filterStateful(CurrentState::new, (cs, event) -> FILTER LAMBDA)
         *
         * Where cs is the instance of CurrentState related to this key
         *       event is the Tuple2 input event
         *
         * Note:
         *    When the incoming value is not equal to the previous value, don't forget to update the CurrentState
         *    object with the new value!
         *
         * See:
         *    "filterStateful" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStageWithKey.html
         */
        StreamStage<Tuple2<String,String>> changedLabels = null;

        /*
         * Finally, we can sink the results directly to the "machine_controls" map.  Tuple2<K,V> also implements
         * Map.Entry<K,V> so we can just supply it directly to the IMap Sink.
         *
         * INPUT: Tuple2<String,String>  i.e. (serialNumber, red/orange/green)
         * OUTPUT: None
         *
         * Create a Sink for the "machine_controls" map using Sinks.map (see reference) then finish the pipeline with
         * changedLabels.writeTo(machineControlsSink);
         *
         * See:
         *    "map" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/Sinks.html
         */
        Sink<Map.Entry<String,String>> machineControlsSink = null;

        return pipeline;
    }
    public static void main(String []args){
        //TODO set auto-offset-reset
        Pipeline pipeline = createPipeline();
        pipeline.setPreserveOrder(true);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Temperature Monitor");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
