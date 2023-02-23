package hazelcast.platform.labs;

import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class DynamicMapConfigTask implements Callable<Boolean>, HazelcastInstanceAware , Serializable {
    public DynamicMapConfigTask(MapConfig config){
        this.mapConfig = config;
    }
    private transient HazelcastInstance hz;
    private final MapConfig mapConfig;

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hz = hazelcastInstance;
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        try {
            hz.getConfig().addMapConfig(mapConfig);
        } catch(InvalidConfigurationException icx){
            result = false;
        }
        return result;
    }
}
