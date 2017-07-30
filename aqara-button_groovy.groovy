{\rtf1\ansi\ansicpg1252\cocoartf1504\cocoasubrtf830
{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\paperw11900\paperh16840\margl1440\margr1440\vieww10800\viewh8400\viewkind0
\pard\tx566\tx1133\tx1700\tx2267\tx2834\tx3401\tx3968\tx4535\tx5102\tx5669\tx6236\tx6803\pardirnatural\partightenfactor0

\f0\fs24 \cf0 /**\
 *  Xiaomi Zigbee Button\
 *\
 *  Modified by RaveTam from Eric Maycock implementation below. Added to support Holdable Button and Battery status reporting\
 *  https://github.com/erocm123/SmartThingsPublic/blob/master/devicetypes/erocm123/xiaomi-smart-button.src/xiaomi-smart-button.groovy\
 *\
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except\
 *  in compliance with the License. You may obtain a copy of the License at:\
 *\
 *      http://www.apache.org/licenses/LICENSE-2.0\
 *\
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed\
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License\
 *  for the specific language governing permissions and limitations under the License.\
 *\
 */\
metadata \{\
	definition (name: "Aqara Button Switch Test", namespace: "lazcad", author: "RaveTam") \{\
		capability "Battery"\
        capability "Button"\
        capability "Refresh"\
        \
        attribute "lastPress", "string"\
        attribute "batterylevel", "string"\
	\}\
    \
    simulator \{\
   	  status "button 1 pressed": "on/off: 1"\
      status "button 1 released": "on/off: 0"\
    \}\
    \
\
	tiles(scale: 2) \{\
    	standardTile("button", "device.button", decoration: "flat", width: 2, height: 2) \{\
        	state "default", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"\
        \}\
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) \{\
			state "battery", label:'$\{currentValue\}% battery', unit:""\
		\}\
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) \{\
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"\
        \}\
\
		main (["button"])\
		details(["button", "battery", "refresh"])\
	\}\
\}\
\
def parse(String description) \{\
  log.debug "Parsing '$\{description\}'"\
  def value = zigbee.parse(description)?.text\
  log.debug "Parse: $value"\
  def descMap = zigbee.parseDescriptionAsMap(description)\
  def results = []\
  if (description?.startsWith('on/off: '))\
		results = parseCustomMessage(description)\
  if (description?.startsWith('catchall:')) \
		results = parseCatchAllMessage(description)\
        \
  return results;\
\}\
\
\
def refresh()\{\
	"st rattr 0x$\{device.deviceNetworkId\} 1 2 0"\
    "st rattr 0x$\{device.deviceNetworkId\} 1 0 0"\
	log.debug "refreshing"\
    \
    createEvent([name: 'batterylevel', value: '100', data:[buttonNumber: 1], displayed: false])\
\}\
\
private Map parseCatchAllMessage(String description) \{\
	Map resultMap = [:]\
	def cluster = zigbee.parse(description)\
	log.debug cluster\
	if (cluster) \{\
		switch(cluster.clusterId) \{\
			case 0x0000:\
			resultMap = getBatteryResult(cluster.data.last())\
			break\
\
			case 0xFC02:\
			log.debug 'ACCELERATION'\
			break\
\
			case 0x0402:\
			log.debug 'TEMP'\
				// temp is last 2 data values. reverse to swap endian\
				String temp = cluster.data[-2..-1].reverse().collect \{ cluster.hex1(it) \}.join()\
				def value = getTemperature(temp)\
				resultMap = getTemperatureResult(value)\
				break\
		\}\
	\}\
\
	return resultMap\
\}\
\
private Map getBatteryResult(rawValue) \{\
	log.debug 'Battery'\
	def linkText = getLinkText(device)\
\
	log.debug rawValue\
    \
    int battValue = rawValue\
\
	def result = [\
		name: 'battery',\
		value: battValue,\
        unit: "%",\
        isStateChange:true,\
        descriptionText : "$\{linkText\} battery was $\{rawValue\}%"\
	]\
    \
    log.debug result.descriptionText\
    state.lastbatt = new Date().time\
    return createEvent(result)\
\}\
\
private Map parseCustomMessage(String description) \{\
	if (description?.startsWith('on/off: ')) \{\
    	if (description == 'on/off: 0') 		//button pressed\
    		return createPressEvent(1)\
    	else if (description == 'on/off: 1') 	//button pressed\
    		return createPressEvent(1)\
	\}\
\}\
\
//this method determines if a press should count as a push or a hold and returns the relevant event type\
private createButtonEvent(button) \{\
	def currentTime = now()\
    def startOfPress = device.latestState('lastPress').date.getTime()\
    def timeDif = currentTime - startOfPress\
    def holdTimeMillisec = (settings.holdTime?:3).toInteger() * 1000\
    \
   \
    	return createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)\
\
\}\
\
private createPressEvent(button) \{\
    	return createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)\
	//return createEvent([name: 'lastPress', value: now(), data:[buttonNumber: button], displayed: true])\
\
\}\
\
//Need to reverse array of size 2\
private byte[] reverseArray(byte[] array) \{\
    byte tmp;\
    tmp = array[1];\
    array[1] = array[0];\
    array[0] = tmp;\
    return array\
\}\
\
private String swapEndianHex(String hex) \{\
    reverseArray(hex.decodeHex()).encodeHex()\
\}}