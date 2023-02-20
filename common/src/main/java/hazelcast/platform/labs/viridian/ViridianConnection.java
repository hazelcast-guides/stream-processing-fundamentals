package hazelcast.platform.labs.viridian;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.SSLConfig;

import java.io.File;
import java.util.Properties;

public class ViridianConnection {
    public static final String VIRIDIAN_SECRETS_DIR_PROP = "VIRIDIAN_SECRETS_DIR";
    public static final String VIRIDIAN_CLUSTER_ID_PROP = "VIRIDIAN_CLUSTER_ID";
    public static final String VIRIDIAN_PASSWORD_PROP = "VIRIDIAN_PASSWORD";
    public static final String VIRIDIAN_DISCOVERY_TOKEN_PROP = "VIRIDIAN_DISCOVERY_TOKEN";

    private static String getRequiredEnv(String envVarName){
        String result = System.getenv(envVarName);
        if (result == null)
            throw new RuntimeException("Required environment variable (" + envVarName + ") was not provided.");

        return result;
    }

    /*
     * Looks for Viridian configuration in the environment
     */
    public static boolean viridianConfigPresent(){
        String secretsDir = System.getenv(VIRIDIAN_SECRETS_DIR_PROP);
        return secretsDir != null;
    }
    public static void configureFromEnvironment(ClientConfig clientConfig){
        String secretsDir = getRequiredEnv(VIRIDIAN_SECRETS_DIR_PROP);
        String password = getRequiredEnv(VIRIDIAN_PASSWORD_PROP);
        String clusterId = getRequiredEnv(VIRIDIAN_CLUSTER_ID_PROP);
        String discoveryToken = getRequiredEnv(VIRIDIAN_DISCOVERY_TOKEN_PROP);
        configure(clusterId, discoveryToken, password, secretsDir, clientConfig);
    }

    public static void configure(String clusterId, String discoveryToken, String password, String secretsDir, ClientConfig clientConfig){
        File configDir = new File(secretsDir);
        if (!configDir.isDirectory()){
            throw new RuntimeException("Could not initialize Viridian connection because the given secrets directory (" + secretsDir + ") does not exist or is not a directory.");
        }

        File keyStoreFile = new File(configDir, "client.keystore");
        File trustStoreFile = new File(configDir, "client.truststore");
        if (!keyStoreFile.isFile() || !keyStoreFile.canRead()){
            throw new RuntimeException("The keystore file (" + keyStoreFile.getPath() +") was not found or could not be read.");
        }
        if (!trustStoreFile.isFile() || !trustStoreFile.canRead()){
            throw new RuntimeException("The truststore file (" + trustStoreFile.getPath() +") was not found or could not be read.");
        }

        Properties props = new Properties();
        props.setProperty("javax.net.ssl.keyStore", keyStoreFile.getPath());
        props.setProperty("javax.net.ssl.keyStorePassword", password);
        props.setProperty("javax.net.ssl.trustStore", trustStoreFile.getPath());
        props.setProperty("javax.net.ssl.trustStorePassword", password);

        clientConfig.getNetworkConfig().setSSLConfig(new SSLConfig().setEnabled(true).setProperties(props));
        clientConfig.getNetworkConfig().getCloudConfig().setEnabled(true).setDiscoveryToken(discoveryToken);
        clientConfig.setProperty("hazelcast.client.cloud.url", "https://api.viridian.hazelcast.com");
        clientConfig.setClusterName(clusterId);
    }
}
