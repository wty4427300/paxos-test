package com;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import proto.Paxoskv;
import proto.PaxosKVGrpc;

public class KvClient{

    public static final long AcceptorBasePort = 3333;

    private Paxoskv.Proposer p;

    public KvClient(Paxoskv.Proposer p) {
        this.p = p;
    }

    public void Phase1(long[] acceptorIds, int  quorum) {
        List<Paxoskv.Acceptor> replies = this.rpcToAll(acceptorIds, "Prepare");
        for (Paxoskv.Acceptor reply : replies) {
            if (reply.get.getV() > p.getBal().getV()) {
                p = Paxoskv.Proposer.newBuilder(p).setBal(reply.getVBal()).build();
            }
        }
    }

    /**
     * @param acceptorIds id数组
     * @param action      prepare/Accept
     */
    public List<Paxoskv.Acceptor> rpcToAll(long[] acceptorIds, String action) {
        List<Paxoskv.Acceptor> replies = new ArrayList<>();
        for (long aid : acceptorIds) {
            String address = String.format("127.0.0.1:%d", AcceptorBasePort + aid);
            ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilder(address, InsecureChannelCredentials.create());
            ManagedChannel channel = channelBuilder.build();
            PaxosKVGrpc.PaxosKVBlockingStub blockingStub = PaxosKVGrpc.newBlockingStub(channel);

            try {
                Paxoskv.Acceptor reply = Paxoskv.Acceptor.newBuilder().build();
                if (action.equals("prepare")) {
                    reply = blockingStub.prepare(this.p);
                } else if (action.equals("Accept")) {
                    reply = blockingStub.accept(this.p);
                }
                if (reply != null) {
                    replies.add(reply);
                }
            } finally {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return replies;
    }
}
