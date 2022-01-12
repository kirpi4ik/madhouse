package org.myhab.jobs

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.joda.time.DateTime
import org.myhab.ConfigKey
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.events.TopicName
import org.myhab.init.cache.CacheMap
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

/**
 * SwitchOFF peripheral after some timeout, also check if there is some peripheral in status ON but without cached expiration
 */
@DisallowConcurrentExecution
@Transactional
class SwitchOFFOnTimeoutJob implements Job, EventPublisher {
    HazelcastInstance hazelcastInstance;

    static triggers = {
        simple repeatInterval: 10000
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        checkCacheAndSwitchOffAfterTimeout(context)
        checkOnPeripheralAndSetTimeoutValueIfNeeded(context)

    }

    void checkOnPeripheralAndSetTimeoutValueIfNeeded(JobExecutionContext jobExecutionContext) {
        DevicePort.findAllByValue("ON").each { port ->
            boolean cached = false
            hazelcastInstance.getMap(CacheMap.EXPIRE).entrySet().each { candidateForExpiration ->
                if (candidateForExpiration.key.equals(String.valueOf(port.id))) {
                    cached = true
                    return true
                }
            }
            if (!cached) {
                def peripheral = port.peripherals[0]
                if (peripheral != null) {
                    def config = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT }
                    if (config != null) {
                        def expireInMs = DateTime.now().plusSeconds(Integer.valueOf(config.value)).toDate().time
                        hazelcastInstance.getMap(CacheMap.EXPIRE).put(String.valueOf(port.id), [expireOn: expireInMs, peripheralId: peripheral.id])
                    }
                }
            }
        }
    }

    def checkCacheAndSwitchOffAfterTimeout(JobExecutionContext context) {
        hazelcastInstance.getMap(CacheMap.EXPIRE).entrySet().each { candidateForExpiration ->
            def objToExpire = candidateForExpiration?.value
            def now = DateTime.now()
            if (objToExpire?.peripheralId != null && now.isAfter(objToExpire?.expireOn)) {
                publish(TopicName.EVT_LIGHT.id(), [
                        "p0": TopicName.EVT_LIGHT.id(),
                        "p1": EntityType.PERIPHERAL.name(),
                        "p2": objToExpire?.peripheralId,
                        "p3": "timeout",
                        "p4": "off",
                        "p6": "system"
                ])
                hazelcastInstance.getMap(CacheMap.EXPIRE).remove(String.valueOf(candidateForExpiration?.key))
            }
        }
    }
}
