/**
 *	Aeon Home Energy Meter v2 Gen2 Basic Edition
 *	Version: 0.9f
 *
 *	Disclaimer: This WILL NOT work with Aeon's HEM Gen1 or Gen5 (latest version) as is intended to be used for HEMs
 *				installed on the typical 200A 240V split-phase US residential systems (Two 120V legs and a neutral -
 *				grounded center tap from power transformer outside your house).
 *
 *	Copyright 2016 Alex M. Ruffell
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *	Author: Alex M. Ruffell
 *
 *	Note:	This device handler is based off of Smartthings' "Aeon Smart Meter" device handler sample code but mostly on
 *			a device handler written by Barry A. Burke (Thank you!!). I have made very significant changes to his code,
 *			stripped stuff, and added other stuff but the overall structure likely remains (for now at least).
 *
 *	Goal:	I removed all support for v1 (sorry!) to keep the code simple and pertinent to what I thought was the latest
 *			version of the HEM - v2, but I just realized Aeon released Gen5 with zwave plus. I wanted to have a device
 *			handler that fully supported Android as I would never even dream of buying any Apple products! I also wanted
 *			a way to keep track of my usage to get the best deal out of my energy provider's contract. Calculating cost
 *			was of no use to me given the price per kWh changes based on my consumption. In my case, if I am between
 *			1000kWh and 2000kWh in a single month, I get a $100 credit on my account so this device handler just has a
 *			counter you can reset monthly. Last, I removed one of the digits after the decimal as it is generally superfluous
 *			for typical HEM applications and it is also likely that the accuracy of the HEM doesn't guarantee the accuracy of
 *			the measurement anyway (at least not over time given there is no calibration feature). In other words the extra digit
 *			is of no use if not accurate.
 *
 *	To Do:	- Possibly add back some of the min/max functionality but I am not sure how useful that would be
 *			- Remove all kVAh support. Anybody use this for home energy tracking??
 *			- Once debug on/off is enabled, add more debugging to help troubleshoot issues
 *			- Check whether polling is needed, and enable if needed
 *			- Refresh and Configure button may not be necessary, evaluate and leave/remove as needed
 *			- Look into sendEvent and [name.... commands. Not sure they are all required.
 *
 *	History:
 * 
 *	2016-07-15:	- Basic functionality seems to work but lots more work is necessary
 *	2016-07-19:	- Change max values used for tile colors. I want the tile to be red before it gets to max values. If it goes beyond the
 *				  new lower max it will either just stay red. All steps in between are interpolated so this change just makes it transition
 *				  to red sooner.
 *				- Got rid of all foreground color code as it does not do anything (on Android at the very least)
 *				- Added V adjustment in case the meter's readings are off
 *	2016-07-31: - Updated fingerprint to new format (possibly only implemented on Hub v2 at this time)
 *	2016-08-16	- Commented out V_L1 and V_L2 code as there never are any values. null values were causing an 'ambiguity' error.
 *				- Commented out ReportGroup3 code as it is disabled in configuration. Not necessary as RG1 and 2 give better control.
 *				- Adding better ability to control what debug messages print to the log. Developers and non developers may have different needs.
 *				- Fixed "Recently" log. Icons now show for all log entries.
 *	2016-09-15	- Got rid of decimal values for voltage tile coloring as it was really unnecessary
 *				- Few minor changes to help with debugging why the DTH stops reporting data on occasion. Research whether it is large W numebrs over 10k.
 *				- Added code to convert report group delay integers to hex
 *	2016-10-05	- Changed V/W/A/kWh variables from string to number (they are numbers after all!) in hopes it might fix the occasional freezing issue.
 *	2016-10-13	- Fixed range check so tiles seem to update properly now. May have fixed freezing as well.
 *				- Fixed date resetting feature
 *				- Changed voltage range scale so that color changes will represent better the critical nature of the voltage swing. 114-126 is +-5% of 120 and is considered acceptable.
 *	2016-11-18	- Fixed display issues now that SmartThings for Android 2.2.2 has been released
 *				- Added ability to enter an adjustment value for the energy meter so you can keep track of power consumption from last bill you received. Use last reading on bill,
 *				  then check current reading, reset the counter and enter the difference in the settings. This way, the "Adjusted Meter" should be quite close to what the utility will
 *				  bill you at the end of the month.
 *				- 
 *				- 
 *
 *	Disclaimer 2:	I am NOT a developer. I learn as I go so please do NOT rely on this for anything critical. Use it and
 *					change it as needed but not for commercial purposes. I will not make any changes to this code that fix things on iOS
 *					if it breaks anything on Android - sorry! Also, I barely got to this point so adding new features may be out of my reach for now.
 *
 *
 */
metadata {
	definition (
		name: 		"Aeon HEMv2 Gen2 - AMR Basic Edition", 
		namespace: 	"Green Living",
		category: 	"Green Living",
		author: 	"Alex M. Ruffell",
        
		// ************************************************************************
		// * Icons - for now random icons I found until I find a better fit
		// ************************************************************************
    
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
	)
    
	{
		capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
		capability "Refresh"
		//capability "Polling"

		//attribute "energy",			"number"		// Sum of energy used on both legs, total energy used by house (defined by capability)
		//attribute "power",			"number"		// Sum of power from both legs, total power used by house (defined by capability)
		//attribute "amps",				"number"		// Sum of amperage from both legs, total power used by house (defined by capability)
		//attribute "volts",			"number"		// Volts of both legs, total power used by house (defined by capability)
		
        attribute "E_METER",		"number"		// Sum last meter reading to current E_L1_L2 counter to get current meter reading
        attribute "ADJ_METER",		"number"		// Meter adjustment so you can keep track of monthly usage even without resetting the meter at the same time as your service provider.

		attribute "E_L1_L2",		"number"		// Sum of energy (kWh) used on both legs, total energy used by house
		attribute "E_L1",			"number"		// Energy from leg 1
		attribute "E_L2",			"number"		// Energy from leg 2

		attribute "W_L1_L2",		"number"		// Sum of power from both legs, total power used by house
		attribute "W_L1",			"number"		// Power from leg 1
		attribute "W_L2",			"number"		// Power from leg 2

		attribute "V_L1_L2",		"number"		// Volts for leg 1 and 2 - voltage on L1 and L2 should always be the same, if not there is an issue!
        attribute "V_L1",			"number"		// Volts for leg 1 - voltage on L1 and L2 should always be the same, if not there is an issue!
        attribute "V_L2",			"number"		// Volts for leg 1 - voltage on L1 and L2 should always be the same, if not there is an issue!

		attribute "A_L1_L2",		"number"		// Sum of amerage used on both legs, total amperage used by house
		attribute "A_L1",			"number"		// Amperage for leg 1
		attribute "A_L2",			"number"        // Amperage for leg 2

		attribute "resetDate",		"string"		// Date kWh was reset. This helps keep track consumption is sync with power company meter readings

		command "reset"
		command "configure"
		command "refresh"
		command "resetCtr"
		//command "poll"

		// Fingerprint for Aeon HEMv2, Second Generation
        //fingerprint deviceId: "0x3101", inClusters: "0x70,0x32,0x60,0x85,0x56,0x72,0x86"
        
        /*
        Z-Wave Command Classes
        Hex id from https://graph.api.smartthings.com/ide/doc/zwave-utils.html

		Model: Home Energy Meter G2 (DSB28-ZWUS)
		Z-Wave Certification Number: ZC08-12090011
		Supported Command Classes 
  
        	0x85 Association 
        	0x20 Basic 
        	0x70 Configuration 
        	0x56 CRC16 Encapsulation 
        	0x72 Manufacturer Specific 
        	0x32 Meter 
        	0x60 Multi Channel 
        	0x86 Version 

        Raw description from IDE: 	zw:L type:3101 mfr:0086 prod:0002 model:001C ver:1.17 zwv:3.67 lib:03 cc:70,32,60,85,56,72,86 epc:2 ep:['3101 32']
        */
        
        // New zwave fingerprint format for Aeon HEMv2, Second Generation (Hub v2 only as of Aug 2016)
        fingerprint mfr: "0086", prod: "0002", model: "001C"
        fingerprint type: "3101", cc: "70,32,60,85,56,72,86"
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 10000; i += 1000) {
			status "W_L1_L2  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 33, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "E_L1_L2  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 33, scale: 0, size: 4).incomingMessage()
		}
	}

	// ************************************************************************
	// * Tile Definitions
	// ************************************************************************

	// scale set to 2 so tiles can be set to anything between 1 and 6 wide
	tiles (scale:2) {

	// ************************************************************************
	// * Watts tiles
	// ************************************************************************

		valueTile("W_L1_L2", "device.W_L1_L2", width: 2, height: 2) {
			state (
				"W_L1_L2", 
				label:'${currentValue} W', 
				backgroundColors:[
                // W values here could go from 0 to 24000 for a 200A service residence however
                // the icon should turn red before it reaches the max, so I am reducing the range
                // to 0 to 18kW so each step is 3kW.
					[value: 0, 		color: "#006600"],	// Dark Green
					[value: 3000, 	color: "#009900"],	// Lighter Green
					[value: 6000, 	color: "#00cc00"],	// Light Green
					[value: 9000, 	color: "#99cc00"],	// Green, touch of yellow
					[value: 12000, 	color: "#ffcc00"],	// Yellow
					[value: 15000, 	color: "#ff6600"],	// Orange
					[value: 18000, 	color: "#ef221a"]	// Red
				
					/* Original colors, taken from ST examples
					[value: 0, 		color: "#153591"],
					[value: 3000, 	color: "#1e9cbb"],
					[value: 6000, 	color: "#90d2a7"],
					[value: 9000, 	color: "#44b621"],
					[value: 12000, 	color: "#f1d801"],
					[value: 18000, 	color: "#d04e00"], 
					[value: 24000, 	color: "#bc2323"]*/
				]
			)
		}
		valueTile("W_L1", "device.W_L1", width: 3, height: 2) {
			state(
				"W_L1", 
				label:'${currentValue} W',
				backgroundColors:[
                // W values here could go from 0 to 24000 for a 200A service residence however
                // the icon should turn red before it reaches the max, so I am reducing the range
                // to 0 to 18kW so each step is 3kW.
					[value: 0, 		color: "#006600"],	// Dark Green
					[value: 3000, 	color: "#009900"],	// Lighter Green
					[value: 6000, 	color: "#00cc00"],	// Light Green
					[value: 9000, 	color: "#99cc00"],	// Green, touch of yellow
					[value: 12000, 	color: "#ffcc00"],	// Yellow
					[value: 15000, 	color: "#ff6600"],	// Orange
					[value: 18000, 	color: "#ef221a"]	// Red
				]
			)
		}
		valueTile("W_L2", "device.W_L2", width: 3, height: 2) {
			state(
				"W_L2", 
				label:'${currentValue} W', 
				backgroundColors:[
                // W values here could go from 0 to 24000 for a 200A service residence however
                // the icon should turn red before it reaches the max, so I am reducing the range
                // to 0 to 18kW so each step is 3kW.
					[value: 0, 		color: "#006600"],	// Dark Green
					[value: 3000, 	color: "#009900"],	// Lighter Green
					[value: 6000, 	color: "#00cc00"],	// Light Green
					[value: 9000, 	color: "#99cc00"],	// Green, touch of yellow
					[value: 12000, 	color: "#ffcc00"],	// Yellow
					[value: 15000, 	color: "#ff6600"],	// Orange
					[value: 18000, 	color: "#ef221a"]	// Red
				]
			)
		}

	// ************************************************************************
	// * Energy tiles
	// ************************************************************************

		valueTile("resetDate", "device.resetDate", width: 3, height: 1) {
			state(
				"resetDate",
				label: 'Since: ${currentValue}', 
				backgroundColor: "#ffffff")
		}    
		valueTile("E_L1_L2", "device.E_L1_L2", width: 3, height: 1/*, canChangeIcon: true*/) {
			state(
				"E_L1_L2",
				label: '${currentValue} kWh', 
				backgroundColor: "#ffffff")
		}
		valueTile("E_L1", "device.E_L1", width: 3, height: 1) {
			state(
				"E_L1",
				label: 'L1: ${currentValue} kWh', 
				backgroundColor: "#ffffff")
		}        
		valueTile("E_L2", "device.E_L2", width: 3, height: 1) {
			state(
				"E_L2",
				label: 'L2: ${currentValue} kWh', 
				backgroundColor: "#ffffff")
		}
        valueTile("E_METER", "device.E_METER", width: 6, height: 2) {
			state(
				"E_METER",
				label: 'Adjusted Meter: ${currentValue} kWh', 
				backgroundColor: "#ffffff")
		} 

	// ************************************************************************
	// * Voltage tile - Just one as voltage is the same on both legs
	// ************************************************************************
	
		valueTile("V_L1_L2", "device.V_L1_L2", width: 2, height: 2) {
			state(
				"V_L1_L2",
				label: '${currentValue} V', 
				backgroundColors:[
                	//Nominal voltage is 120V +-5% so 114 to 126 are within acceptable range. I set 115 to 125 so it is RED a bit sooner.
                    //I did not spread the values out evenly as being on exactly 120V is not critical so making it always something other
                    //than GREEN would reduce the user's focus on something is wrong when voltage is actually too high or low.
                    //Grows YELLOW slowly and then RED faster with this spread.
					[value: 115, 	color: "#ef221a"],	// Red
					[value: 116, 	color: "#ffcc00"],	// Yellow
					[value: 120, 	color: "#006600"],	// Green
					[value: 124, 	color: "#ffcc00"],	// Yellow
					[value: 125, 	color: "#ef221a"]	// Red
				]
			)
		}


	// ************************************************************************
	// * Amperage tiles
	// ************************************************************************

		valueTile("A_L1_L2", "device.A_L1_L2", width: 2, height: 2) {
			state (
				"A_L1_L2",
				label: '${currentValue} A' , 
				backgroundColors:[
                // A values here could go from 0 to 200 for a 200A serivce residence however
                // the icon should turn red before it reaches the max, so I am reducing the range
                // to 0 to 150A so each step is 25A.
					[value: 0,		color: "#006600"],	// Dark Green
					[value: 25, 	color: "#009900"],	// Lighter Green
					[value: 50, 	color: "#00cc00"],	// Light Green
					[value: 75, 	color: "#99cc00"],	// Green, touch of yellow
					[value: 100,	color: "#ffcc00"],	// Yellow
					[value: 125,	color: "#ff6600"], 	// Orange
					[value: 150,	color: "#ef221a"]	// Red
				]
			)
		}
		valueTile("A_L1", "device.A_L1", width: 3, height: 2) {
			state(
				"A_L1",
				label:'${currentValue} A',
				backgroundColors:[
                // A values here could go from 0 to 200 for a 200A serivce residence however
                // the icon should turn red before it reaches the max, so I am reducing the range
                // to 0 to 150A so each step is 25A.
					[value: 0,		color: "#006600"],	// Dark Green
					[value: 25, 	color: "#009900"],	// Lighter Green
					[value: 50, 	color: "#00cc00"],	// Light Green
					[value: 75, 	color: "#99cc00"],	// Green, touch of yellow
					[value: 100,	color: "#ffcc00"],	// Yellow
					[value: 125,	color: "#ff6600"], 	// Orange
					[value: 150,	color: "#ef221a"]	// Red
				]
			)
		}
		valueTile("A_L2", "device.A_L2", width: 3, height: 2) {
			state(
				"A_L2",
				label:'${currentValue} A',
				backgroundColors:[
                // A values here could go from 0 to 200 for a 200A serivce residence however
                // the icon should turn red before it reaches the max, so I am reducing the range
                // to 0 to 150A so each step is 25A.
					[value: 0,		color: "#006600"],	// Dark Green
					[value: 25, 	color: "#009900"],	// Lighter Green
					[value: 50, 	color: "#00cc00"],	// Light Green
					[value: 75, 	color: "#99cc00"],	// Green, touch of yellow
					[value: 100,	color: "#ffcc00"],	// Yellow
					[value: 125,	color: "#ff6600"], 	// Orange
					[value: 150,	color: "#ef221a"]	// Red
				]
			)
		}

	// ************************************************************************	
	// * CONTROL TILES
	// *
	// * Using decoration set to flat only because it enables visual feedback
	// * of button being pressed
	// * 
	// * Using background color as it makes it more obvious it is a button,
	// * and it delimites the space for text making it a bit more uniform
	// ************************************************************************

		// Reset Button - Clear display and start over (will not reset kWh counter)
		standardTile("reset", "command.reset", inactiveLabel: false, /*decoration: "flat",*/ width: 2, height: 2) {
			state "default", label:'Reset', action:"reset", icon: "st.Health & Wellness.health7", backgroundColor: "#bcccac"
		}

		// Reset kWh Button - Clear display, reset kWh counter and start over
		standardTile("resetCtr", "device.reset_ctr", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
			state "default", label:'Reset kWh Counter', action:"resetCtr", icon: "st.Office.office10"
		}

		// Refresh Button
		standardTile("refresh", "command.refresh", inactiveLabel: false, /*decoration: "flat",*/ width: 2, height: 2) {
			state "default", label:'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon", backgroundColor: "#bcccac" 
		}

		// Configure Button
		standardTile("configure", "command.configure", inactiveLabel: false, /*decoration: "flat",*/ width: 2, height: 2) {
			state "configure", label:'Config', action:"configuration.configure", icon:"st.secondary.tools", backgroundColor: "#bcccac"
		}


	// ************************************************************************
	// * Which tiles are displayed and in what order
	// ************************************************************************

		main ([
			"E_L1_L2"/*,
			"W_L1_L2",
			"A_L1_L2",
			"V_L1_L2"*/
			])
        
		details([
			"V_L1_L2","W_L1_L2","A_L1_L2",
			"W_L1","W_L2",
			"A_L1","A_L2",
			"resetDate", "E_L1_L2",
			"E_L1","E_L2",
            "E_METER",
			"reset","refresh","configure",
			"resetCtr"
			])
	}

	// ************************************************************************
	// * User selectable preferences for Device Handler
	// ************************************************************************

	// Setting a default value (defaultValue: "foobar") for an input may render that selection in the mobile app,
	// but the user still needs to enter data in that field. It’s recommended to not use defaultValue to avoid confusion.

	// Stuff disabled below is not fully functional yet or needed
    
	preferences {
		input name: "reportGroup1",		type: "number", title: "Update A/V/W every x seconds", 	description: "Enter desired seconds", 			defaultValue: 60, 		displayDuringSetup: false
		input name: "reportGroup2",		type: "number", title: "Update kWh every x seconds", 	description: "Enter desired seconds", 			defaultValue: 78, 		displayDuringSetup: false
		/*input name: "reportGroup3", 	type: "number", title: "Update A/V/W/KWH for L1/L2/Whole HEM (L1+L2) every x seconds", description: "Enter desired seconds", defaultValue: 78, displayDuringSetup: false*/ //Disabled configuration
        input name: "vAdjustment",		type: "number", title: "Voltage adjustment (+/-x)", 	description: "Enter adjustment amount +/-x",	defaultValue: 0,		displayDuringSetup: false
		input name: "debugOnOff",		type: "enum", 	title: "Debug log messages", 			description: "", options: ["on", "off"],		defaultValue: "on",		displayDuringSetup: false
        input name: "devDebugOnOff",	type: "enum", 	title: "Developer Debug log messages", 	description: "", options: ["on", "off"],		defaultValue: "off",	displayDuringSetup: false
        input name: "lastMeterReading",	type: "number", title: "Last Meter Reading (kWh)",		description: "Enter kWh from last bill",		defaultValue: 0,		displayDuringSetup: false
        input name: "meterAdjustment",	type: "number", title: "Meter Adjustment (kWh)",		description: "Enter kWh used between last bill and last time the meter was reset",		defaultValue: 0,		displayDuringSetup: false
	}

}

	// ************************************************************************
	// * installed() - Called when an instance of the app is installed.
    // * Typically subscribes to events from the configured devices and 
    // * creates any scheduled jobs.
	// ************************************************************************

def installed() {
	log.debug "installed with settings: $settings"	//No code to enable/disable debug messages as this is run right after install.
	configure()					// Send new configuration settings
    reset()						// Clear all values in case there is space junk on screen
	refresh()					// Force new measurements and update display
}

	// ************************************************************************
	// * updated() - Called when the preferences of an installed app are
    // * updated. Typically unsubscribes and re-subscribes to events from the
    // * configured devices and unschedules/reschedules jobs.
	// ************************************************************************

def updated() {
	if (settings.devDebugOnOff == "on") {log.debug "preferences updated with settings: $settings"}
	configure()					// Send new configuration settings
	newMeasurements()
    refresh()					// Force new measurements and update display

}

	// ************************************************************************
	// * parse
	// ************************************************************************

def parse(String description) {
	if (settings.devDebugOnOff == "on") {log.debug "Parse received ${description}"}
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result) { 
		if (settings.debugOnOff == "on") {log.debug "Parse returned ${result?.descriptionText}"}
		return result
	} else {
	}
}

	// ************************************************************************
	// * zwaveEvent
	// ************************************************************************
    
def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	def newValue
    def adjValue
	def formattedValue
	def MAX_AMPS = 220				// This exceeds typical residential split-phase panel amerage on purpose, cuts off values that are too high (fluke in reading)
	def MAX_WATTS = 26400			// This exceeds typical residential split-phase panel power on purpose, cuts off values that are too high (fluke in reading)
    def Integer vAdj = settings.vAdjustment as Integer
    def Integer eAdj = settings.meterAdjustment as Integer
    def Integer meter = settings.lastMeterReading as Integer
    //def Integer day = now.date
    
    // Just to be sure vAdj is 0 if preference was not set.
    if (vAdj == null) {
		vAdj = 0
	}
    

	// def timeStamp =  new Date().format("HH:MM - dd-MMM-yy", location.timeZone)
	if (cmd.meterType == 33) {
		if (cmd.scale == 0) {
			newValue = Math.round(cmd.scaledMeterValue * 100) / 100
			formattedValue = String.format("%5.1f", newValue)
			if (newValue != state.E_L1_L2) {
				state.E_L1_L2 = newValue
                sendEvent(name: "E_L1_L2", value: formattedValue, unit: "", descriptionText: "Total Energy: ${formattedValue} kWh"/*, displayed: false*/)
				[name: "E_L1_L2", value: formattedValue, unit: "kWh", descriptionText: "Total Energy: ${formattedValue} kWh"]
                //[name: "energy", value: newValue, unit: "kWh", descriptionText: "Total Energy: ${formattedValue} kWh"]
                
                adjValue = newValue + eAdj
                state.E_METER = adjValue
                formattedValue = String.format("%5.1f", adjValue)
                sendEvent(name: "E_METER", value: formattedValue, unit: "", descriptionText: "Adjusted Meter: ${formattedValue} kWh")
			}
		} 
		
		else if (cmd.scale == 1) {
        	if (settings.devDebugOnOff == "on") {log.debug "This section of code was commented out - Err1 E_L1_L2 kVAh"}
			/* Do nothing as the DTH is not handling kVA
            newValue = Math.round(cmd.scaledMeterValue * 100) / 100
			formattedValue = String.format("%5.1f", newValue)
			if (newValue != state.E_L1_L2) {
				sendEvent(name: "E_L1_L2", value: formattedValue, unit: "", descriptionText: "Display Energy: ${formattedValue} kVAh", displayed: false)
				state.E_L1_L2 = formattedValue
				[name: "energy", value: newValue, unit: "kVAh", descriptionText: "Total Energy: ${formattedValue} kVAh"]
			}*/
		}
		else if (cmd.scale == 2) {
			newValue = Math.round(cmd.scaledMeterValue)
			formattedValue = newValue as String
            if (newValue < 0 || newValue > MAX_WATTS ) {
            	//New value outside of acceptable range therefore not OK to display. Log value for debugging purposes.
            	if (settings.debugOnOff == "on") {log.debug "ERROR: Out of range W_L1_L2 value: ${formattedValue} W"}
            }
            else {
            		//New value within acceptable range therefore OK to display
                    if (newValue != state.W_L1_L2) {
						state.W_L1_L2 = newValue
                        sendEvent(name: "W_L1_L2", value: formattedValue, unit: "", descriptionText: "Total Power: ${formattedValue} W"/*, displayed: false*/)
						[name: "W_L1_L2", value: formattedValue, unit: "W", descriptionText: "Total Power: ${formattedValue} W"] // This makes mini tile appear in "Recently" log <<< not working, why??
                		//[name: "power", value: newValue, unit: "W", descriptionText: "Total Power: ${newValue} W"] //Likely can be deleted if correction above doesn't break anything
                	}
			}
		}
	}
	else if (cmd.meterType == 161) {
		if (cmd.scale == 0) {
			newValue = Math.round(cmd.scaledMeterValue * 100) / 100 + vAdj		// Round, add +/- adjustment
			formattedValue = String.format("%5.1f", newValue)
			if (newValue != state.V_L1_L2) {
				state.V_L1_L2 = newValue
                sendEvent(name: "V_L1_L2", value: formattedValue, unit: "", descriptionText: "Voltage: ${formattedValue} V"/*, displayed: false*/)              
				[name: "V_L1_L2", value: formattedValue, unit: "V", descriptionText: "Volts: ${formattedValue} V"] // This makes mini tile appear in "Recently" log
                //[name: "volts", value: newValue, unit: "V", descriptionText: "Volts: ${formattedValue} V"] //Likely can be deleted if correction above doesn't break anything
			}
		}
		else if (cmd.scale == 1) {
			newValue = Math.round( cmd.scaledMeterValue * 100) / 100
            formattedValue = String.format("%5.1f", newValue)
			if (newValue < 0 || newValue > MAX_AMPS) {
            	//New value outside of acceptable range therefore not OK to display. Log value for debugging purposes.
				if (settings.debugOnOff == "on") {log.debug "ERROR: Out of range A_L1_L2 value: ${formattedValue} A"}
            }
            else {
	            	//New value within acceptable range therefore OK to display
					if (newValue != state.A_L1_L2) {
						state.A_L1_L2 = newValue
                        sendEvent(name: "A_L1_L2", value: formattedValue, unit: "", descriptionText: "Total Current: ${formattedValue} A"/*, displayed: false*/)            
						[name: "A_L1_L2", value: formattedValue, unit: "A", descriptionText: "Total Current: ${formattedValue} A"] // This makes mini tile appear in "Recently" log
						//[name: "amps", value: newValue, unit: "A", descriptionText: "Total Current: ${formattedValue} A"] //Likely can be deleted if correction above doesn't break anything           	
                 	}
			}
		}
	}           
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def newValue
	def formattedValue
	def MAX_AMPS = 220				// This exceeds typical residential split-phase panel amerage on purpose, cuts off values that are too high (fluke in reading)
	def MAX_WATTS = 26400			// This exceeds typical residential split-phase panel power on purpose, cuts off values that are too high (fluke in reading)
    def Integer vAdj = settings.vAdjustment as Integer
    
    // Just to be sure vAdj is 0 if preference was not set.
    if (vAdj == null) {
		vAdj = 0
	}
    
	if (cmd.commandClass == 50) {    
		def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1]) // can specify command class versions here like in zwave.parse
		if (encapsulatedCommand) {
			// This section handles values from Clamp 1 (EndPoint 1)
			if (cmd.sourceEndPoint == 1) {
				if (encapsulatedCommand.scale == 2 ) {
					newValue = Math.round(encapsulatedCommand.scaledMeterValue)
                    formattedValue = newValue as String
					if (newValue < 0 || newValue > MAX_WATTS ) {
                    	//New value outside of acceptable range therefore not OK to display. Log value for debugging purposes
                    	if (settings.debugOnOff == "on") {log.debug "ERROR: Out of range W_L1 value: ${formattedValue} W"}
                    }
                    else {
							//New value within acceptable range therefore OK to display
							if (newValue != state.W_L1) {
								state.W_L1 = newValue
                                //sendEvent(name: "W_L1", value: formattedValue, unit: "", descriptionText: "L1 Power: ${formattedValue} W"/*, displayed: false*/)    << is this needed?
								[name: "W_L1", value: formattedValue, unit: "", descriptionText: "L1 Power: ${formattedValue} W"]
							}
                    }
				} 
				else if (encapsulatedCommand.scale == 0 ){
						newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
						formattedValue = String.format("%5.1f", newValue)
						if (newValue != state.E_L1) {
							state.E_L1 = newValue
							[name: "E_L1", value: formattedValue, unit: "", descriptionText: "L1 Energy: ${formattedValue} kWh"]
						}
				}
				else if (encapsulatedCommand.scale == 1 ){
                	if (settings.devDebugOnOff == "on") {log.debug "This section of code was commented out - Err2 E_L1 kVAh"}
					/*
                    newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
					formattedValue = String.format("%5.1f", newValue)
					if (formattedValue != state.E_L1) {
						state.E_L1 = formattedValue
						[name: "E_L1", value: formattedValue, unit: "", descriptionText: "L1 Energy: ${formattedValue} kVAh"]
					}*/
				}
				else if (encapsulatedCommand.scale == 5 ) {
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
                    formattedValue = String.format("%5.1f", newValue)
					if (newValue < 0 || newValue > MAX_AMPS ) {
                    	//New value outside of acceptable range therefore not OK to display. Log value for debugging purposes.
                    	if (settings.debugOnOff == "on") {log.debug "ERROR: Out of range A_L1 value: ${formattedValue} A"}
                    }
                    else {
                    		//New value within acceptable range therefore OK to display
                            if (newValue != state.A_L1) {
								state.A_L1 = newValue
								[name: "A_L1", value: formattedValue, unit: "", descriptionText: "L1 Current: ${formattedValue} A"]
							}
                    }
				}
				else if (encapsulatedCommand.scale == 4 ){
                			//This section only updates the log, not the tiles
                    		newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100 + vAdj
							formattedValue = String.format("%5.1f", newValue)
                            if (formattedValue != state.V_L1) {
								state.V_L1 = formattedValue
								[name: "V_L1", value: formattedValue, unit: "", descriptionText: "L1 Voltage: ${formattedValue} V"]
							}
					}
			}
			// This section handles values from Clamp 2 (EndPoint 2)
			else if (cmd.sourceEndPoint == 2) {
				if (encapsulatedCommand.scale == 2 ){
					newValue = Math.round(encapsulatedCommand.scaledMeterValue)
					if (newValue < 0 || newValue > MAX_WATTS ) {
                    	//New value outside of acceptable range therefore not OK to display. Log value for debugging purposes.
                    	if (settings.debugOnOff == "on") {log.debug "ERROR: Out of range W_L2 value: ${formattedValue} W"}
                    }
                    else {
							//New value within acceptable range therefore OK to display
                            formattedValue = newValue as String
							if (newValue != state.W_L2) {
								state.W_L2 = newValue
								[name: "W_L2", value: formattedValue, unit: "", descriptionText: "L2 Power: ${formattedValue} W"]
                            }
					}
				}
				else if (encapsulatedCommand.scale == 0 ){
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
					formattedValue = String.format("%5.1f", newValue)
					if (newValue != state.E_L2) {
						state.E_L2 = newValue
						[name: "E_L2", value: formattedValue, unit: "", descriptionText: "L2 Energy: ${formattedValue} kWh"]
					}
				}
				else if (encapsulatedCommand.scale == 1 ){
                	if (settings.devDebugOnOff == "on") {log.debug "This section of code was commented out - Err E_L2 kVAh"}
					/*
                    newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
					formattedValue = String.format("%5.1f", newValue)
					if (newValue != state.E_L2) {
						state.E_L2 = newValue
						[name: "E_L2", value: formattedValue, unit: "", descriptionText: "L2 Energy: ${formattedValue} kVAh"]
					}*/
				}
				else if (encapsulatedCommand.scale == 5 ){
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
                    formattedValue = String.format("%5.1f", newValue)
					if (newValue < 0 || newValue > MAX_AMPS ) {
                    	//New value outside of acceptable range therefore not OK to display. Log value for debugging purposes.
                    	if (settings.debugOnOff == "on") {log.debug "ERROR: Out of range A_L2 value: ${formattedValue} A"}
                    }
                    else {
                    		//New value within acceptable range therefore OK to display
							if (newValue != state.A_L2) {
								state.A_L2 = newValue
								//sendEvent(name: "A_L2", value: formattedValue, unit: "", descriptionText: "L2 Current: ${formattedValue} A") // Is this needed?
                                [name: "A_L2", value: formattedValue, unit: "", descriptionText: "L2 Current: ${formattedValue} A"]
                            }
					}
				}
				else if (encapsulatedCommand.scale == 4 ){
                    newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100 + vAdj
					formattedValue = String.format("%5.1f", newValue)
					if (formattedValue != state.V_L2) {
						state.V_L2 = formattedValue
						[name: "V_L2", value: formattedValue, unit: "", descriptionText: "L2 Voltage: ${formattedValue} V"]
					}
				}
			}
		}
	}
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	if (settings.devDebugOnOff == "on") {log.debug "Unhandled event ${cmd}"}
	[:]
}

// Read new values and display them on screen. Do not reset anything.
def refresh() {
	if (settings.devDebugOnOff == "on") {log.debug "refresh()"}
    
	newMeasurements()		// Read new values from meter
	updateDisplay()			// Send new values to display
}

/*
def poll() {
	log.debug "poll()"
	refresh()
}
*/

def updateDisplay() {
	
    if (settings.devDebugOnOff == "on") {log.debug "updateDisplay()"} // - E_L1_L2: ${state.E_L1_L2} kWh"}	//change to any variable
	
    sendEvent(name: "V_L1_L2", value: state.V_L1_L2, unit: "")    
	sendEvent(name: "W_L1_L2", value: state.W_L1_L2, unit: "")

	sendEvent(name: "W_L1", value: state.W_L1, unit: "")
    sendEvent(name: "W_L2", value: state.W_L2, unit: "")

    sendEvent(name: "A_L1_L2", value: state.A_L1_L2, unit: "")
    sendEvent(name: "A_L1", value: state.A_L1, unit: "")
    sendEvent(name: "A_L2", value: state.A_L2, unit: "")
    sendEvent(name: "E_L1_L2", value: state.E_L1_L2, unit: "")
    sendEvent(name: "E_L1", value: state.E_L1, unit: "")
    sendEvent(name: "E_L2", value: state.E_L2, unit: "")

	sendEvent(name: "resetDate", value: state.resetDate, unit: "")

}

	// ************************************************************************
	// * Request new values from HEM without resetting the device
	// ************************************************************************

def newMeasurements() {
      
	def cmd = delayBetween( [
		zwave.meterV2.meterGet(scale: 0).format(),					// 0 = Requests kWh values; 1 = Requests kVAh values
		zwave.meterV2.meterGet(scale: 2).format(),					// Requests W values
		zwave.meterV2.meterGet(scale: 4).format(),					// Requests V values
		zwave.meterV2.meterGet(scale: 5).format()					// Requests A values
        
	], 1000)
    cmd
    }

	// ************************************************************************
    // * Reset - This mostly resets values and what is shown on the display in case it seems stuck or there are other issues.
    // * It is not meant to reset the meter deleting the running counter, there is another method for that.
    // ************************************************************************

def reset() {
	if (settings.devDebugOnOff == "on") {log.debug "reset()"}

    // Reset state attributes
    state.E_L1_L2 =	0
    state.E_L1 =	0
    state.E_L2 =	0
    state.W_L1_L2 =	0
    state.W_L1 =	0
    state.W_L2 =	0
    state.A_L1_L2 =	0
    state.A_L1 =	0
    state.A_L2 =	0
    state.V_L1_L2 =	0
    
    // Clear tiles
	sendEvent(name: "E_L1_L2",		value: 0, unit: "")
    sendEvent(name: "E_L1",			value: 0, unit: "")
    sendEvent(name: "E_L2",			value: 0, unit: "")
    sendEvent(name: "W_L1_L2",		value: 0, unit: "")
    sendEvent(name: "W_L1",			value: 0, unit: "")
    sendEvent(name: "W_L2",			value: 0, unit: "")
    sendEvent(name: "A_L1_L2",		value: 0, unit: "")
    sendEvent(name: "A_L1",			value: 0, unit: "")
    sendEvent(name: "A_L2",			value: 0, unit: "")
    sendEvent(name: "V_L1_L2",		value: 0, unit: "")

	sendEvent(name: "resetDate",	value: null, unit: "")
    
    configure()											// Send configuration parameters
    
    newMeasurements()									// Request new values
    
    updateDisplay()										// Send new values to display

}

// Reset the energy counter and the date tracking the last reset
def resetCtr() {
	if (settings.devDebugOnOff == "on") {log.debug "resetCtr()"}
    def dateString = new Date().format("dd-MMM-yy", location.timeZone)
    //def timeString = new Date().format("HH:MM", location.timeZone)    // Leaving this here just in case I want to add time as well

    // Reset state attributes
    state.E_L1_L2 =	0
    state.E_L1 =	0
    state.E_L2 =	0
    state.resetDate = dateString
    
    // Clear tiles
	sendEvent(name: "E_L1_L2",		value: 0, unit: "")
    sendEvent(name: "E_L1",			value: 0, unit: "")
    sendEvent(name: "E_L2",			value: 0, unit: "")	

    sendEvent(name: "resetDate", value: state.resetDate, unit: "")	
    //updateDisplay()
    
    return zwave.meterV2.meterReset().format()					// Reset all meter values not state values, without return it was not actually executing... is it now skipping updateDisplay()?
}



	// ************************************************************************
    // * Configure the Aeon HEM device with desired settings
    // ************************************************************************

def configure() {
	if (settings.devDebugOnOff == "on") {log.debug "configure()"}
    
    //String hex = Integer.toHexString(val);
	//int parsedResult = (int) Long.parseLong(hex, 16);
    
	Integer rg1Delay = settings.reportGroup1 as Integer
    Integer rg2Delay = settings.reportGroup2 as Integer
    Integer rg3Delay = settings.reportGroup3 as Integer
    
    // Do values need to be Hex? 0x78 is 120s
    
    if (rg1Delay == null) {
		rg1Delay = 120
	}

	if (rg2Delay == null) {
		rg2Delay = 60
	}
    
    if (rg3Delay == null) {
		rg3Delay = 120
	}
    
    //def str_rg1Delay = Integer.toHexString(rg1Delay)
    //def str_rg2Delay = Integer.toHexString(rg2Delay)
    //def str_rg3Delay = Integer.toHexString(rg3Delay)
    

    
    
	def cmd = delayBetween([

	    /*
	    100 Set 101-103 to default. Default: N/A Size: 1
		101 Which reports need to send in Report group 1 (See flags in table below). Default: 0x00 00 00 02 Size: 4
		102 Which reports need to send in Report group 2 (See flags in table below). Default: 0x00 00 00 01 Size: 4
		103 Which reports need to send in Report group 3 (See flags in table below). Defualt: 0 Size: 4
		110 Set 111-113 to default. Default: N/A Size: 1
		111 The time interval of sending Report group 1 (Valid values 0x01-0xFFFFFFFF). Default: 0x00 00 00 05 Size: 4
		112 The time interval of sending Report group 2 (Valid values 0x01-0xFFFFFFFF). Default: 0x00 00 00 78 Size: 4
		113 The time interval of sending Report group 3 (Valid values 0x01-0xFFFFFFFF). Default: 0x00 00 00 78 Size: 4
		*/
        
        // Disabled Report Group 3 as it is not needed. If enabled, that report is set to send EVERYTHING that is already sent by RG1 and RG2.

		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 0).format(),				// Disable (=0) selective reporting
		//zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: 5).format(),			// Don't send whole HEM unless watts have changed by 30
		//zwave.configurationV1.configurationSet(parameterNumber: 5, size: 2, scaledConfigurationValue: 5).format(),			// Don't send L1 Data unless watts have changed by 15
		//zwave.configurationV1.configurationSet(parameterNumber: 6, size: 2, scaledConfigurationValue: 5).format(),			// Don't send L2 Data unless watts have changed by 15
		//zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: 1).format(),			// Or by 5% (whole HEM)
		//zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, scaledConfigurationValue: 1).format(),			// Or by 5% (L1)
		//zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: 1).format(),			// Or by 5% (L2)
        
		//zwave.configurationV1.configurationSet(parameterNumber: 100, size: 1, scaledConfigurationValue: 0).format(),			// reset to default 101 to 103
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 1776399).format(),  	// Report Group 1: A/V/W/KWH for L1/L2/Whole HEM (L1+L2)
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 6145).format(),			// Report Group 2: KWH for L1/L2/Whole HEM (L1+L2)
		//zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 1776399).format(),	// Report Group 3: A/V/W/KWH for L1/L2/Whole HEM (L1+L2)
        zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),			// Report Group 3: A/V/W/KWH for L1/L2/Whole HEM (L1+L2)
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: rg1Delay).format(),		// Send Report Group 1 every x seconds
		//zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 30).format(), 		// Send Report Group 1 every 30 seconds
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: rg2Delay).format(),		// Send Report Group 2 every x seconds
		//zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 60).format(), 		// Send Report Group 2 every 60 seconds
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: rg3Delay).format() 		// Send Report Group 3 every x seconds
        //zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 30).format() 			// Send Report Group 3 every 30 seconds
	], 2000)																													// 2000ms delay between commands
	if (settings.debugOnOff == "on") {log.debug "Configuration: ${cmd}"}

	cmd
}