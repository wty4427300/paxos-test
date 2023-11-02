package com;

import java.util.Collections;
import java.util.Map;

public class KVServer {
    private Mutex mutex;
    /**
     * 存储多个值的多个版本
     */
    private Map<String, Map<Long, Version>> storage;

    public Version getLockedVersion(Paxoskv.PaxosInstanceId id) {
        this.mutex.lock();
        String key = id.getKey();
        long ver = id.getVer();
        Map<Long, Version> s = this.storage.get(key);
        if(s!=null && !s.isEmpty()){

        }
        return null;
    }
}
