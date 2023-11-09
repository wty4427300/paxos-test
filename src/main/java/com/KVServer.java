package com;

import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

public class KVServer extends PaxosKVGrpc.PaxosKVImplBase {
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


    @Override
    public void accept(Paxoskv.Proposer request, StreamObserver<Paxoskv.Acceptor> responseObserver) {
        Version v = this.getLockedVersion(request.getId());
        Paxoskv.BallotNum d = v.getAcceptor().getLastBal();
        Paxoskv.Acceptor reply = Paxoskv.Acceptor.newBuilder()
                .setLastBal(d)
                .build();
        //初始化返回值
        if (this.ge(request.getBal(), v.getAcceptor().getLastBal())) {
            v.getAcceptor().newBuilderForType()
                    .setLastBal(request.getBal())
                    .setVal(request.getVal())
                    .setVBal(request.getBal());
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void prepare(Paxoskv.Proposer request, StreamObserver<Paxoskv.Acceptor> responseObserver) {
        Version v = this.getLockedVersion(request.getId());
        Paxoskv.Acceptor reply = v.getAcceptor();
        if (this.ge(request.getBal(), v.getAcceptor().getLastBal())) {
            v.getAcceptor().newBuilderForType().setLastBal(request.getBal());
        }
        v.getMu().unlock();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    public boolean ge(Paxoskv.BallotNum a, Paxoskv.BallotNum b) {
        if (a.getN() > b.getN()) {
            return true;
        }
        if (a.getN() < b.getN()) {
            return false;
        }
        return a.getProposerId() >= b.getProposerId();
    }
}
