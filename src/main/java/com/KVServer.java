package com;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

public class KVServer {
    private Lock mu;
    /**
     * 存储多个值的多个版本
     */
    private Map<String, Map<Long, Version>> storage;

    public KVServer() {
        this.mu = new Mutex();
        this.storage = new HashMap<>();
    }

    public Version getLockedVersion(Paxoskv.PaxosInstanceId id) {
        this.mu.lock();
        String key = id.getKey();
        long ver = id.getVer();
        //
        Map<Long, Version> s = this.storage.get(key);
        if (Objects.isNull(s)) {
            s = new HashMap<>();
            this.storage.put(key, s);
        }
        Version v = s.get(ver);
        if (Objects.isNull(v)) {
            v = new Version();
            Paxoskv.Acceptor acceptor = Paxoskv.Acceptor.newBuilder().build();
            v.setAcceptor(acceptor);
        }
        v.getMu().lock();
        return v;
    }

    public Paxoskv.Acceptor Accept(Paxoskv.Proposer proposer){
        return null;
    }
}
