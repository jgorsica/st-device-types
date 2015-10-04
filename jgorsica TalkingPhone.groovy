/**
 *  Talking Phone
 *
 *  Author: John Gorsica
 *
 *Speech Synthesis sends a command to the talking phone via SharpTools Plugin for Tasker
 *Speech Recognition is done by AutoVoice tasker plugin on device and then set in this device via SharpTools Tasker Plugin command call.
 *Playlist (or song or genre or radio) set in this device via SharpTools Tasker Plugin command call.
 *Music control for this phone performed by sending a command to the talking phone via SharpTools Plugin for Tasker
 *Music state and current track are sent from Media Utilities Plugin via SharpTools Plugin
 *
 */
 
preferences {
    //input("autoRemoteKey", "string", title:"Enter AutoRemote Key:",
    //    required:true, displayDuringSetup: true)
}

metadata {
	// Automatically generated. Make future change here.
	definition (name: "Talking Phone", namespace: "jgorsica", author: "John Gorsica") {
        capability "Speech Synthesis"
        capability "Speech Recognition"
        capability "Music Player"
        command "__testTTS"
        command "setPlaylist", ["string"]
        command "setPhraseSpoken", ["string"]
        command "eventFromPhone" , ["string"]
        command "tileSetLevel", ["number"]
        attribute "commandToPhone", "string"
	}

	// Main
    standardTile("main", "device.status", inactiveLabel:false, decoration:"flat") {
            state "default", label:"Phone", icon:"st.People.people11", action:"__testTTS"
    }
    standardTile("play", "device.status", width: 1, height: 1, decoration: "flat") {
		state "default", label:'', action:"music Player.play", icon:"st.sonos.play-btn", nextState:"playing", backgroundColor:"#ffffff"
	}
	standardTile("nextTrack", "device.status", width: 1, height: 1, decoration: "flat") {
		state "next", label:'', action:"music Player.nextTrack", icon:"st.sonos.next-btn", backgroundColor:"#ffffff"
	}
	
	standardTile("previousTrack", "device.status", width: 1, height: 1, decoration: "flat") {
		state "previous", label:'', action:"music Player.previousTrack", icon:"st.sonos.previous-btn", backgroundColor:"#ffffff"
	}
    standardTile("pause", "device.status", width: 1, height: 1, decoration: "flat") {
		state "default", label:'', action:"music Player.pause", icon:"st.sonos.pause-btn", nextState:"paused", backgroundColor:"#ffffff"
    }
    standardTile("stop", "device.status", width: 1, height: 1, decoration: "flat") {
		state "default", label:'', action:"music Player.stop", icon:"st.sonos.stop-btn", nextState:"stopped", backgroundColor:"#ffffff"
    }
	controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
		state "level", action:"tileSetLevel", backgroundColor:"#ffffff"
	}
    standardTile("toggle", "device.status", width: 1, height: 1, canChangeIcon: true) {
		state "paused", label:'Paused', action:"music Player.play", icon:"st.Electronics.electronics16", nextState:"playing", backgroundColor:"#ffffff"
		state "playing", label:'Playing', action:"music Player.pause", icon:"st.Electronics.electronics16", nextState:"paused", backgroundColor:"#79b821"
	}
    valueTile("currentSong", "device.trackDescription", inactiveLabel: true, height:1, width:3, decoration: "flat") {
		state "default", label:'${currentValue}', backgroundColor:"#ffffff"
	}
    
	main "main"

	details([
    "currentSong",
    "previousTrack","toggle","nextTrack",
	//	"play","pause","stop",
		"levelSliderControl"
	])
}

def sendCommandToPhone(header, command) {
    def temp = header + "=:=" + command
    sendEvent(name:"commandToPhone",value:temp,isStateChange:true)
}

def speak(text) {
    def header = 'say'
    sendCommandToPhone(header, text)
}

def play() {
	log.trace "play()"
	def header = 'media play'
    sendCommandToPhone(header, '')
}

def stop() {
	log.trace "stop()"
	def header = 'media stop'
    sendCommandToPhone(header, '')}

def pause() {
	log.trace "pause()"
	def header = 'media pause'
    sendCommandToPhone(header, '')
}

def nextTrack() {
	log.trace "nextTrack()"
	def header = 'media next'
    sendCommandToPhone(header, '')
}

def previousTrack() {
	log.trace "previousTrack()"
	def header = 'media previous'
    sendCommandToPhone(header, '')
}

def setPlaylist(String playlist) {
	log.trace "setPlaylist:" + playlist
	def header = 'media playlist'
    sendCommandToPhone(header, playlist)
}

def tileSetLevel(Number level) {
	def volumeStep = "";
	if (level<=6){
    	volumeStep="0";
    } else if(level<=12){
    	volumeStep="1";
    } else if(level<=18){
		volumeStep="2";
    } else if(level<=24){
		volumeStep="3";
    } else if(level<=31){
		volumeStep="4";
    } else if(level<=37){
		volumeStep="5";
    } else if(level<=43){
		volumeStep="6";
    } else if(level<=50){
		volumeStep="7";
    } else if(level<=56){
		volumeStep="8";
    } else if(level<=62){
		volumeStep="9";
    } else if(level<=68){
		volumeStep="10";
    } else if(level<=75){
		volumeStep="11";
    } else if(level<=81){
		volumeStep="12";
    } else if(level<=87){
		volumeStep="13";
    } else if(level<=93){
		volumeStep="14";
	} else if(level<=100){
		volumeStep="15";
	}
    log.trace "setVolume($volumeStep)"
    def header = 'media volume'
    sendCommandToPhone(header, volumeStep)
}

def eventFromPhone(String event) {
	log.trace "event from phone: $event"
    def eventSplit = event.split("=:=")
    def eventName=eventSplit[0]
    if (eventName=="playState"){
    	def isPlaying
        if(eventSplit[1]=="true"){
        	def descriptionText = "$device.displayName is playing music"
        	log.info descriptionText
            sendEvent(name: "status",
                value: "playing",
                descriptionText: descriptionText
                )
        } else{
        	def descriptionText = "$device.displayName is not playing music"
        	log.info descriptionText
            sendEvent(name: "status",
                value: "paused",
                descriptionText: descriptionText
                )
        }
    }
    if (eventName=="currentSong"){
    	def currentTrackDescription =eventSplit[1]
        def descriptionText = "$device.displayName current song: $currentTrackDescription"
        log.info descriptionText
    	sendEvent(name: "trackDescription",
            value: currentTrackDescription,
            descriptionText: descriptionText
            )
    }
    if (eventName=="currentVolume"){
    	def currentVolume =((((eventSplit[1].toInteger()+1)*100).intdiv(16))-1)
        def descriptionText = "$device.displayName volume is $currentVolume"
        log.info descriptionText
    	sendEvent(name: "level",
            value: currentVolume,
            descriptionText: descriptionText
            )
    }
    if (eventName=="musicRequest"){
    	//do nothing
    }
    if (eventName=="textResponse"){
    	//do nothing
    }
    //setPhraseSpoken(event)
}

def setPhraseSpoken(String phrase) {
	log.trace "setPhraseSpoken:" + phrase
	phraseSpoken = phrase
}

def __testTTS(String stuff) {
    speak("Hello, my name is Amelia.  I am honored to be your smart home.")
    
}
