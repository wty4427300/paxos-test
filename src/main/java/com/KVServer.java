package com;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import proto.PaxosKVGrpc;
import proto.Paxoskv;

public class KVServer extends PaxosKVGrpc.PaxosKVImplBase {
    private final Lock mu;
    /**
     * 存储多个值的多个版本
     */
    private final Map<String, Map<Long, Version>> storage;

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
        this.mu.unlock();
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
        if (ge(request.getBal(), v.getAcceptor().getLastBal())) {
            v.getAcceptor().newBuilderForType()
                    .setLastBal(request.getBal())
                    .setVal(request.getVal())
                    .setVBal(request.getBal());
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
        v.getMu().unlock();
    }

    @Override
    public void prepare(Paxoskv.Proposer request, StreamObserver<Paxoskv.Acceptor> responseObserver) {
        Version v = this.getLockedVersion(request.getId());
        Paxoskv.Acceptor reply = v.getAcceptor();
        if (ge(request.getBal(), v.getAcceptor().getLastBal())) {
            v.getAcceptor().newBuilderForType().setLastBal(request.getBal());
        }
        v.getMu().unlock();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    public static boolean ge(Paxoskv.BallotNum a, Paxoskv.BallotNum b) {
        if (a.getN() > b.getN()) {
            return true;
        }
        if (a.getN() < b.getN()) {
            return false;
        }
        return a.getProposerId() >= b.getProposerId();
    }

    private static final int ACCEPTOR_BASE_PORT = 5000;

    public static List<Server> serveAcceptors(List<Long> acceptorIds) throws IOException {
        List<Server> servers = new ArrayList<>();

        for (Long aid : acceptorIds) {
            int port = ACCEPTOR_BASE_PORT + aid.intValue();
            Server server = ServerBuilder
                    .forPort(port)
                    .addService(new KVServer())
                    // Enables reflection for gRPC
                    .addService(ProtoReflectionService.newInstance())
                    .build();

            servers.add(server);

            // Start the server in a separate thread
            new Thread(() -> {
                try {
                    System.out.printf("Acceptor-%d serving on port %d...\n", aid, port);
                    server.start();
                    server.awaitTermination();
                } catch (IOException | InterruptedException e) {
                    System.err.printf("Server for acceptor-%d failed to start: %s\n", aid, e.getMessage());
                }
            }).start();
        }
        return servers;
    }
}
