package org.campagnelab.gobyweb.clustergateway.jobs;

import java.util.HashMap;

/**
 * The runtime environment variables visible to a job at execution time.
 *
 * @see Job
 * @see JobBuilder
 *
 * @author manuele
 */
public class JobRuntimeEnvironment extends HashMap<String,Object> {


    @Override
    public Object get(Object key) {
        //we do not need to decorate a key for the get. it is "undecorated" in the OGE script and VariableHelper
        return super.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(decorateKey(key));
    }

    @Override
    public Object put(String key, Object value) {
        return super.put(decorateKey(key), value);
    }

    @Override
    public Object remove(Object key) {
        return super.remove(decorateKey(key));
    }

    private String decorateKey(String key) {
        return "%" + key + "%";
    }

    private String decorateKey(Object key) {
        return "%" + key + "%";
    }
}
