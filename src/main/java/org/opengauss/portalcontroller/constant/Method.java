package org.opengauss.portalcontroller.constant;

public interface Method {
    interface Run{
        String ZOOKEEPER = "runZookeeper";
        String KAFKA = "runKafka";
        String REGISTRY = "runSchemaRegistry";
        String CONNECT_SINK = "runKafkaConnectSink";
        String CONNECT_SOURCE = "runKafkaConnectSource";
        String REVERSE_CONNECT_SINK = "runReverseKafkaConnectSink";
        String REVERSE_CONNECT_SOURCE = "runReverseKafkaConnectSource";
        String CHECK_SOURCE = "runDataCheckSource";
        String CHECK_SINK = "runDataCheckSink";
        String CHECK = "runDataCheck";
    }
    interface Stop{
        String ZOOKEEPER = "stopZookeeper";
        String KAFKA = "stopKafka";
        String REGISTRY = "stopSchemaRegistry";
        String CONNECT_SOURCE = "stopKafkaConnectSource";
        String CONNECT_SINK = "stopKafkaConnectSink";
        String REVERSE_CONNECT_SOURCE = "stopReverseKafkaConnectSource";
        String REVERSE_CONNECT_SINK = "stopReverseKafkaConnectSink";
        String CHECK_SOURCE = "stopDataCheckSource";
        String CHECK_SINK = "stopDataCheckSink";
        String CHECK = "stopDataCheck";
    }
}
