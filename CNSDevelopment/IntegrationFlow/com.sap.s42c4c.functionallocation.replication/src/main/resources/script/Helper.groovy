import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

  //Properties 
  def map = message.getProperties()
  def createdByUser = map.get("p_CreatedByUser")
  def lastChangedByUser = map.get("p_LastChangedByUser")
  def eventType = map.get("p_eventType")
  def commUser = map.get("P_CommUser")

  if (commUser.length() > 0 && eventType.equals('sap.s4.beh.functionallocation.v1.FunctionalLocation.Created.v1') && createdByUser.equals(commUser)) {
    message.setProperty("p_ReplicateToC4C", "N")
  }

  if (commUser.length() > 0 && eventType.equals('sap.s4.beh.functionallocation.v1.FunctionalLocation.Changed.v1') && lastChangedByUser.equals(commUser)) {
    message.setProperty("p_ReplicateToC4C", "N")
  }

  return message
}

def Message prepareCustomQueryForODATACall(Message message) {

  //Properties
  def properties = message.getProperties();
  def client = properties.get("P_Client")
  def lang = properties.get("P_Language")

  if (lang.length() == 0) {
    lang = "EN"
    message.setProperty("P_Language", lang)
  }
  if (client.length() == 0)
    message.setProperty("P_CustomQuery", "sap-language=" + lang)
  else
    message.setProperty("P_CustomQuery", "sap-client=" + client + "&" + "sap-language=" + lang)
  return message
}

def Message enrichJsonOutput(Message message) {
  //Body 
  def body = message.getBody(java.io.Reader);

  JsonSlurper slurper = new JsonSlurper()
  Map parsedJson = slurper.parse(body)

  parsedJson.messageRequests.each {

    if (it.body.isBlocked) {
      it.body.isBlocked = Boolean.parseBoolean(it.body.isBlocked)
    }

    if (it.body.isDeleted) {
      it.body.isDeleted = Boolean.parseBoolean(it.body.isDeleted)
    }
    if (it.body.country.length() == 0) 
      it.body.country = null
    
    if (it.body.postalCode.length() == 0) 
      it.body.postalCode = null
    
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

    it.body.customerInformation.each{
        if (it.languageCode.length() == 0)
            it.languageCode = null
        if (it.content.length() == 0)
            it.content = null    
    }

  }

  def respbody = JsonOutput.toJson(parsedJson)
  message.setBody(respbody)

  return message;
}