import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.json.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

def Message prepareQueryFilter(Message message) {

  def properties = message.getProperties()
  def sapClient = properties.get("Client")
  def languages = properties.get("Language")
  def codeListEntityDesc = properties.get("CodeListEntityDesc")
  def industrySystemType = properties.get("IndustrySystemType")

  def languageArray = languages.split(',')

  def languageFilter = languageArray.collect {
    "Language%20eq%20%27$it%27"
  }.join('%20or%20')

  def textFilter = "\$filter="+codeListEntityDesc+"%20ne%20%27%27" + (languages ? ("%20and%20%28" + languageFilter + "%29") : ("")) + "%20and%20IndustrySystemType%20eq%20%27" + industrySystemType + "%27" + (sapClient ? "&sap-client=$sapClient" : "")

  message.setProperty("P_queryFilter", textFilter)

  return message
}