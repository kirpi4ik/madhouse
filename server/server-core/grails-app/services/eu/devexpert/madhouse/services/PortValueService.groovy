package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.ConfigKey
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.Event
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.port.PortAction
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.domain.events.TopicName
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.init.cache.CacheMap
import eu.devexpert.madhouse.parser.ValueParser
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Propagation

@Slf4j
@Transactional(propagation = Propagation.REQUIRED)
class PortValueService implements EventPublisher {
    @Value('${autoImportPorts}')
    boolean autoImportPorts

    @Value('${autoImportDevices}')
    boolean autoImportDevices

    def hazelcastInstance;
    def deviceService

    @Subscriber('evt_mqtt_port_value_changed')
    def updatePort(event) {
        def device = Device.findByCode(event.data.p2, [lock: true])
        if (device == null && autoImportDevices) {
            device = deviceService.importDevice(event.data.p1, event.data.p2)
        }

        def devicePort = device?.ports?.find {
            port -> port.internalRef == event.data.p4
        }
        if (device != null && devicePort == null && autoImportPorts) {
            devicePort = deviceService.importPort(device, event.data.p3, event.data.p4)
        }
        if (devicePort != null) {
            def newVal = ValueParser.parser(devicePort).apply(event.data.p5)
            if (devicePort.value != newVal) {

                try {
                    PortValue newPortValue = new PortValue()
                    newPortValue.portUid = devicePort.uid
                    newPortValue.value = newVal
                    newPortValue.save(failOnError: false, flush: true)
                    devicePort.setValue(newVal)
                    devicePort.save(failOnError: false, flush: true)

                    publish(TopicName.EVT_PORT_VALUE_PERSISTED.id(), new EventData().with {
                        p0 = TopicName.EVT_PORT_VALUE_PERSISTED.id()
                        p1 = EntityType.PORT.name()
                        p2 = "${devicePort.id}"
                        p3 = "${devicePort.uid}"
                        p4 = "${devicePort.value}"
                        p5 = "mqtt"
                        it
                    })
                } catch (Exception ex) {
                    log.error(ex.message)
                }
                publish(TopicName.EVT_UI_UPDATE_PORT_VALUE.id(), new Event().with {
                    p0 = TopicName.EVT_PORT_VALUE_PERSISTED.id()
                    p1 = EntityType.PORT.name()
                    p2 = "${devicePort.id}"
                    p3 = "${devicePort.uid}"
                    p4 = "${devicePort.value}"
                    p5 = "mqtt"
                    it
                })
            }
        }
    }

    @Subscriber("evt_mqtt_port_value_changed")
    def updateExpirationTime(event) {
        def port = Device.findByCode(event.data.p2, [lock: true])?.ports?.find {
            port -> port.internalRef == event.data.p4
        }
        if (port != null) {
            def newVal = ValueParser.parser(port).apply(event.data.p5)

            if (!port.peripherals.empty) {
                def peripheral = port.peripherals?.first()
                if (peripheral != null) {
                    def config = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT }
                    if (config != null && newVal == PortAction.ON.name()) {
                        def expireInMs = DateTime.now().plusSeconds(Integer.valueOf(config.value)).toDate().time
                        hazelcastInstance.getMap(CacheMap.EXPIRE).put(String.valueOf(port.id), [expireOn: expireInMs, peripheralId: peripheral.id])
                    } else if (newVal == PortAction.OFF.name()) {
                        hazelcastInstance.getMap(CacheMap.EXPIRE).remove(String.valueOf(port.id))
                    }
                }
            }
        }
    }

}
