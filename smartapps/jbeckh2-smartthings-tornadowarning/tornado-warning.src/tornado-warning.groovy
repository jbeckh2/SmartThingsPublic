/**
 *  Tornado Warning
 *
 *  Copyright 2016 Jeremy Beckham
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Tornado Warning",
    namespace: "jbeckh2.smartthings.tornadowarning",
    author: "Jeremy Beckham",
    description: "Pulses a light when there is a tornado warning near you.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Input the Zip Code that we are watching for Tornado Warnings") {
		input "zipCode", "text", title:"Zip Code"
	}
    section("The API Key for WeatherUnderground.  This can be obtained from https://www.wunderground.com/weather/api/.") {
    	input "weatherUndergroundApiKey", "text", title:"WeatherUnderground API Key"
    }
    section("Light bulb options.  (Requires Color Controled Light Bulb)"){
    	input "bulb", "capability.colorControl", title:"Light Bulb"
    }
    
}

def stillPulsing = true
def directionUp = true
def stopTime = new Date()

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug("initializing")
    schedule("0 0/5 10-11 * * ?", checkForTornadoWarning)
}

// TODO: implement event handlers
def checkForTornadoWarning() {
	def apiKey = weatherUndergroundApiKey
    def zip = zipCode
	def baseUrl = "http://api.wunderground.com/api/${apiKey}/alerts/q/${zip}.json"
    
    def jsonTxt = baseUrl.toURL().text
    response = new groovy.json.JsonSlurper().parseText(jsonTxt)
    
    for (alert in response.alerts) {
    	if (alert.type == "TOR") {
        	startPulseRed()
            break
        } else if (alert.type == "TOW") {
        	startPulseBlue()
            break
        }
    }
}

def setStopTime () {
	stopTime = new Date()
    stopTime.minutes += 5
}

def startPulseRed() {
	setStopTime()
	stillPulsing = true
	setPulseLevel(0, 10)
}

def startPulseBlue() {
	setStopTime()
	stillPulsing = true
	setPulseLevel(240, 10)
}

def stopPulsing() {
	bulb.off()
    stillPulsing = false
}

def setBulb(hue, level) {
	if (new Date() > stopTime) {
    	stopPulsing()
    }
    else
    {
        bulb.on()
        bulb.setHue(hue)
        bulb.setSaturation(100)
        bulb.setLevel(level)

        if (stillPulsing) {
            runIn(1000, { 
                if (directionUp) {
                    if (level+10 >= 100) {
                        directionUp = false
                    }
                    setBulb(hue, level+10)
                } else {
                    if (level+10 >= 0) {
                        directionUp = true
                    }
                    setBulb(hue, level-10)
                }
            })
        }
    }
}