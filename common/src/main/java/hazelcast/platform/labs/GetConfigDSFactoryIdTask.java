package hazelcast.platform.labs;

import com.hazelcast.internal.config.ConfigDataSerializerHook;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class GetConfigDSFactoryIdTask implements Callable<Integer>, Serializable {

    @Override
    public Integer call() throws Exception {
        return ConfigDataSerializerHook.F_ID;
    }
}
