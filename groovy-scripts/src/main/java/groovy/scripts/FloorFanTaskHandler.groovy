package groovy.scripts

import nl.openweb.iot.dashboard.service.script.AbstractGroovyTaskHandler
import nl.openweb.iot.monitor.domain.Reading
import nl.openweb.iot.monitor.repository.ReadingRepository
import nl.openweb.iot.wio.WioException
import nl.openweb.iot.wio.domain.Node
import nl.openweb.iot.wio.domain.grove.GroveRelay
import nl.openweb.iot.wio.domain.grove.GroveTempHum
import nl.openweb.iot.wio.domain.grove.GroveTempHumPro
import nl.openweb.iot.wio.scheduling.ScheduledTask
import nl.openweb.iot.wio.scheduling.SchedulingUtils
import nl.openweb.iot.wio.scheduling.TaskContext

class FloorFanTaskHandler extends AbstractGroovyTaskHandler {
    private static double THRESHOLD_TEMPERATURE = 22.5

    @Override
    ScheduledTask.TaskExecutionResult execute(Node node, TaskContext context) throws WioException {
        ScheduledTask.TaskExecutionResult result = SchedulingUtils.secondsLater((int) Math.round(period * 60))

        node.getGroveByType(GroveTempHumPro.class).ifPresent({ tempHum ->
            Reading reading = saveTempAndHumidity(tempHum, context)
            node.getGroveByType(GroveRelay.class).ifPresent({ relay ->
                boolean isHot = reading.getTemperature() >= THRESHOLD_TEMPERATURE
                boolean isOn = relay.readOnOff()
                if (isHot && !isOn) {
                    relay.switchOnOff(true)
                } else if (!isHot && isOn) {
                    relay.switchOnOff(false)
                }
            })
        })
        return result
    }

    private static Reading saveTempAndHumidity(GroveTempHum tempHum, TaskContext context) {
        ReadingRepository repository = context.getBean(ReadingRepository.class)
        double temperature = tempHum.readTemperature()
        double humidity = tempHum.readHumidity()
        Reading reading = new Reading()
        reading.setTemperature(temperature)
        reading.setHumidity(humidity)
        reading.setDate(new Date())
        return repository.save(reading)
    }
}
