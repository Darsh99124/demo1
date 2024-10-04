import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message prepareCustomQueryForODATACall(Message message) {
   
    //Properties
    def properties = message.getProperties();
    def client = properties.get("P_Client")
    def lang = properties.get("P_Language")
    
    if (lang.length() == 0 ){
        lang = "EN"
        message.setProperty("P_Language",lang)
    }
    if (client.length() == 0 )
        message.setProperty("P_CustomQuery", "sap-language="+lang)
    else
        message.setProperty("P_CustomQuery", "sap-client=" + client + "&" + "sap-language=" + lang)
    return message
}

def Message checkChangedBy(Message message) {

  //Properties 
  def map = message.getProperties()
  def createdByUser = map.get("p_CreatedByUser")
  def lastChangedByUser = map.get("p_LastChangedByUser")
  def eventType = map.get("p_eventType")
  def commUser = map.get("P_CommUser")

  if (commUser.length() > 0 && eventType.equals('sap.s4.beh.equipment.v1.Equipment.Created.v1') && createdByUser.equals(commUser)) {
    message.setProperty("p_ReplicateToC4C", "N")
  }
  
  if (commUser.length() > 0 && eventType.equals('sap.s4.beh.equipment.v1.Equipment.Changed.v1') && lastChangedByUser.equals(commUser)) {
    message.setProperty("p_ReplicateToC4C", "N")
  }
  

  return message
}

def Message enrichJsonOutput(Message message) {

  //Body 
  def body = message.getBody(java.io.Reader)
  Map parsedJson = new JsonSlurper().parse(body)

  parsedJson.messageRequests.each {

    if (it.body.country.length() == 0) 
      it.body.country = null
    
    if (it.body.streetPostalCode.length() == 0) 
      it.body.streetPostalCode = null
    
    if (it.body.house.length() == 0) 
      it.body.house = null    

    if (it.body.room.length() == 0) 
      it.body.room = null
    
    if (it.body.building.length() == 0) 
      it.body.building = null    

    if (it.body.streetName.length() == 0) 
      it.body.streetName = null    

    if (it.body.cityName.length() == 0) 
      it.body.cityName = null    

    if (it.body.floor.length() == 0) 
      it.body.floor = null    

    if (it.body.region.length() == 0) 
      it.body.region = null    

    it.body.customerInformation.each{
        if (it.languageCode.length() == 0)
            it.languageCode = null
        if (it.content.length() == 0)
            it.content = null    
    }

  }

  def respbody = JsonOutput.toJson(parsedJson)
  message.setBody(respbody)
  
  //Set the C4C message ID as customer monitoring header
   def messageLog = messageLogFactory.getMessageLog(message);
  	if(messageLog != null){
       
  		def messageHeaderId = message.getProperties().get("P_MessageHeaderId")		
  		if(messageHeaderId != null){
  			messageLog.addCustomHeaderProperty("Replication Message ID", messageHeaderId)
          }
  	}

  return message;

}