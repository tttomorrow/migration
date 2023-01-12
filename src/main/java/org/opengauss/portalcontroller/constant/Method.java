package org.opengauss.portalcontroller.constant;

public interface Method {
    interface Run{
        String ZOOKEEPER = "runZookeeper";
        String KAFKA = "runKafka";
        String REGISTRY = "runSchemaRegistry";
        String CONNECT = "runKafkaConnect";
        String REVERSE_CONNECT = "runReverseKafkaConnect";
        String CHECK_SOURCE = "runDataCheckSource";
        String CHECK_SINK = "runDataCheckSink";
        String CHECK = "runDataCheck";
    }
    interface Stop{
        String ZOOKEEPER = "stopZookeeper";
        String KAFKA = "stopKafka";
        String REGISTRY = "stopSchemaRegistry";
        String CONNECT = "stopKafkaConnect";
        String REVERSE_CONNECT = "stopReverseKafkaConnect";
        String CHECK_SOURCE = "stopDataCheckSource";
        String CHECK_SINK = "stopDataCheckSink";
        String CHECK = "stopDataCheck";
    }
}
