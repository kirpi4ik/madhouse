package org.myhab.jobs

import grails.events.EventPublisher
import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.parser.ValueParser
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@DisallowConcurrentExecution
class DeviceControllerStateSyncJob implements Job, EventPublisher {
    def deviceService
    def portValueService
    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(60)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        Device.withCriteria {
            not {
                eq('status', DeviceStatus.DISABLED)
            }
        }.each { deviceController ->
            def httpSyncSupported = deviceController.getConfigurationByKey(CfgKey.DEVICE.DEVICE_HTTP_SYNC_SUPPORTED)
            if (httpSyncSupported && Boolean.valueOf(httpSyncSupported.value)) {
                try {
                    def portValues = deviceService.readPortValuesFromDevice(deviceController)
                    portValues.each { portVal ->
                        def ports = DevicePort.where {
                            internalRef == portVal.key
                            device.id == deviceController.id
                        }
                        def port = ports ? ports.first() : null

                        if (port != null) {
                            def rtPortVal = ValueParser.parser(port).apply(portVal.value)
                            if (port.value != rtPortVal) {
                                def eventData = new EventData().with {
                                    p0 = TopicName.EVT_ASYNC_PORT_VALUE_CHANGED.id()
                                    p1 = "${deviceController.model}"
                                    p2 = "${deviceController.code}" // device internal code
                                    p3 = "${port.type}" // port type (optional)
                                    p4 = "${port.internalRef}" // port internal code
                                    p5 = "${rtPortVal}" // port value
                                    p6 = "device http sync job"
                                    it
                                }
                                publish(TopicName.EVT_ASYNC_PORT_VALUE_CHANGED.id(), eventData)
                            }
                        }
                    }
                } catch (failedToSync) {
                    log.warn(failedToSync.message)
                }
            }
        }

    }
}
