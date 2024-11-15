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

import static com.KVServer.ge;

public class KvClient {

    public static final long AcceptorBasePort = 3333;

    private Paxoskv.Proposer p;

    public KvClient(Paxoskv.Proposer p) {
        this.p = p;
    }

    public Phase1Response Phase1(long[] acceptorIds, int quorum) {
        List<Paxoskv.Acceptor> replies = this.rpcToAll(acceptorIds, "Prepare");
        int ok = 0;
        Paxoskv.BallotNum higherBal = p.getBal();
        Paxoskv.Acceptor maxVoted = Paxoskv.Acceptor.newBuilder()
                .setVBal(Paxoskv.BallotNum.newBuilder().build()).build();

        for (Paxoskv.Acceptor r : replies) {
            if (!ge(p.getBal(), r.getLastBal())) {
                higherBal = r.getLastBal();
                continue;
            }

            if (ge(r.getVBal(), maxVoted.getVBal())) {
                maxVoted = r;
            }

            ok += 1;

            //达到多数派，Phase1 完成
            if (ok == quorum) {
                return new Phase1Response(maxVoted.getVal());
            }
        }
        //多Proposer并发运行造成冲突，最大ballot number
        return new Phase1Response(higherBal);
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
