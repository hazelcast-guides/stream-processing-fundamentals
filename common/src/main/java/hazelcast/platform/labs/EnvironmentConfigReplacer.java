package hazelcast.platform.labs;

import com.hazelcast.config.replacer.spi.ConfigReplacer;

import java.util.Properties;

public class EnvironmentConfigReplacer implements ConfigReplacer {
    @Override
    public void init(Properties properties) {
        
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public String getReplacement(String s) {
        return null;
    }
}
