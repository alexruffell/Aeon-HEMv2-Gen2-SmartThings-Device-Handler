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