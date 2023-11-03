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

    public Paxoskv.Acceptor Prepare(Paxoskv.Proposer p) {
        Version v = this.getLockedVersion(p.getId());
        Paxoskv.Acceptor reply = v.getAcceptor();
        if (this.ge(p.getBal(), v.getAcceptor().getLastBal())) {
            v.getAcceptor().newBuilderForType().setLastBal(p.getBal());
        }
        v.getMu().unlock();
        return reply;
    }


    public Paxoskv.Acceptor Accept(Paxoskv.Proposer p) {
        Version v = this.getLockedVersion(p.getId());
        Paxoskv.BallotNum d = v.getAcceptor().getLastBal();
        //初始化返回值
        Paxoskv.Acceptor reply = Paxoskv.Acceptor.newBuilder()
                .setLastBal(d)
                .build();
        if (this.ge(p.getBal(),v.getAcceptor().getLastBal())){
            v.getAcceptor().newBuilderForType()
                    .setLastBal(p.getBal())
                    .setVal(p.getVal())
                    .setVBal(p.getBal());
        }
        return reply;
    }

    public boolean ge(Paxoskv.BallotNum a, Paxoskv.BallotNum b) {
        //
        if (a.getN() > b.getN()) {
            return true;
        }
        if (a.getN() < b.getN()) {
            return false;
        }
        return a.getProposerId() >= b.getProposerId();
    }
}
