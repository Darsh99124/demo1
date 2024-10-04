import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message enrichJsonOutput(Message message) {
  //Body 
  def body = message.getBody(java.io.Reader);

  JsonSlurper slurper = new JsonSlurper()
  Map parsedJson = slurper.parse(body)
  
  parsedJson.messageRequests.each {
      it.body.isPricingDeterminationRelevant = Boolean.parseBoolean(it.body.isPricingDeterminationRelevant)
      it.body.isRebateRelevant = Boolean.parseBoolean(it.body.isRebateRelevant)
      
  }
    
  // handle xml nil
  def respbody = JsonOutput.toJson(parsedJson)
  def body2 = respbody.replace('""','null')
  def body3 = body2.replace('{}','null')
  message.setBody(body3)
  
  return message;
}