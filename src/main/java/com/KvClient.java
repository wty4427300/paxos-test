package com;

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import proto.Paxoskv;
import proto.PaxosKVGrpc;

public class KvClient {

    public static final long AcceptorBasePort=3333;

    public void Phase1(){

    }

    public void rpcToAll(long[] acceptorIds,String action){
        List<Paxoskv.Acceptor> replies=new ArrayList<>();
        for(long acceptorId:acceptorIds){
            String address=String.format("127.0.0.1:%d",AcceptorBasePort+acceptorId);
            ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilder(address, null);
            ManagedChannel channel = channelBuilder.build();
            PaxosKVGrpc.PaxosKVBlockingStub blockingStub = PaxosKVGrpc.PaxosKVBlockingStub.newStub(null,channel);
        }
    }
}
