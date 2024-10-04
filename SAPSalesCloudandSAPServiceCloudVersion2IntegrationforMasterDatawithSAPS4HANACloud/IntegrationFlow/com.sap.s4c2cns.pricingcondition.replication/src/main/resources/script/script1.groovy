import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {
  //Body 
  def body = message.getBody(java.io.Reader);
  Map parsedJson = new JsonSlurper().parse(body)

  parsedJson.messageRequests.each {

    //body
      if (it.body.isDeleted) {
        it.body.isDeleted = Boolean.parseBoolean(it.body.isDeleted)
      }

  }
  def respbody = JsonOutput.toJson(parsedJson)
  message.setBody(respbody)
  return message;
}