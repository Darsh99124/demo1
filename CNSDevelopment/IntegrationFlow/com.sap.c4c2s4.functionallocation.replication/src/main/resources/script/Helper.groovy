import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.MarkupBuilder;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import java.time.Instant;




/**
    * If a partner function in C4C exists and not in S/4 then it is considered as a create (POST) case
    * If a partner function exists in both C4C and S/4 but the partner no is different, then it is considered as an update (PATCH) case
    * MarkupBuilder is used to construct the batch output. 
    * Each partner function to be added or updated are added to batch as a separate batchChangeSetPart under batchParts/batchChangeSet
    * URI for PATCH Method is of format:
    *       PATCH EquipmentPartner(Equipment='10006851',PartnerFunction='RE',EquipmentPartnerObjectNmbr='2') HTTP/1.1
    * URI for POST Method is of format:
    *       POST EquipmentPartner HTTP/1.1
*/


def Message encodeFunctionalLocationID(Message message) {

  def map = message.getProperties()
  def funLocNo = map.get("P_FunLocNum")

  if (funLocNo.charAt(0) == '?') message.setProperty('P_FunLocNum', '%253F'.concat(funLocNo.substring(1, funLocNo.length())))
  if (funLocNo.charAt(0) == '?') message.setProperty('P_PartnerFunLocNum', '%3F'.concat(funLocNo.substring(1, funLocNo.length())))

  return message

}

def Message setFunctionalLocationProperties(Message message) {
  //Body 
  def body = message.getBody(java.io.Reader)
  Map parsedJson = new JsonSlurper().parse(body)
  
  //set monitoring id
  message.setHeader("SAP_ApplicationID", parsedJson.messageHeader.id)
  
  message.setProperty("P_SenderSequenceNo", parsedJson.messageHeader.creationDateTime)
  message.setProperty("P_FunctionalLocationID", parsedJson.messageRequests[0].body.displayId)
  //Alternate Labelling  enabled
  if (!(parsedJson.messageRequests[0].body.receiverTechnicalId == null)) {

     def temp_P_FunLocNum = parsedJson.messageRequests[0].body.receiverTechnicalId;
     temp_P_FunLocNum = "%253F".concat temp_P_FunLocNum.substring(1, temp_P_FunLocNum.length());
     message.setProperty("P_FunLocNum" ,temp_P_FunLocNum);
     
     def temp_P_PartnerFunLocNum = parsedJson.messageRequests[0].body.receiverTechnicalId;
     temp_P_PartnerFunLocNum = "%3F".concat temp_P_PartnerFunLocNum.substring(1, temp_P_PartnerFunLocNum.length());
     message.setProperty("P_PartnerFunLocNum" ,temp_P_PartnerFunLocNum);
  }
  //Alternate Labelling disabled
  else {
    if (!(parsedJson.messageRequests[0].body.receiverDisplayId == null)) {
      message.setProperty("P_FunLocNum", parsedJson.messageRequests[0].body.receiverDisplayId)
    } else {
      message.setProperty("P_FunLocNum", "")
    }
  }
  //Set Long Text
  if (parsedJson.toString().contains("customerInformation")) {
    message.setProperty("P_LongText", parsedJson.messageRequests[0].body.customerInformation[0].content)
  }else{
       message.setProperty("P_LongText","")
  }

  message.setProperty("P_SenderParty", parsedJson.messageHeader.senderCommunicationSystemDisplayId)
  message.setProperty("P_ReceiverParty", parsedJson.messageHeader.receiverCommunicationSystemDisplayId)

  return message
}

def Message processPartnerFunction(Message message) {

  def s4body = message.getBody(java.io.Reader)
  def s4Party = new XmlSlurper().parse(s4body)

  def map = message.getProperties()
  def funLocNo = map.get("P_PartnerFunLocNum")
  def s4_FunctionalLocation = ""
  def c4cParty = new XmlSlurper().parseText(map.get("P_C4C_InvolvedParties"))

  def patchlist = []
  def postlist = []

  for (party in c4cParty.Party) {

    def partnerRole = party.PartnerFunction.toString()
    def partnerNo = party.Partner.toString()
    def existingParty = false

   // FunctionalLocationType.to_Partner.FunctionalLocationPartnerType , batchPartResponse.batchQueryPartResponse.body.
    for (sparty in s4Party.batchQueryPartResponse.body.FunctionalLocationPartner.FunctionalLocationPartnerType) {

      def s4PartnerRole = sparty.PartnerFunction.toString()
      def s4PartnerNo = sparty.Partner.toString()
      s4_FunctionalLocation = sparty.FunctionalLocation.toString()

      if (s4PartnerRole.equals(partnerRole) && s4PartnerNo.equals(partnerNo)) existingParty = true

      if (s4PartnerRole.equals(partnerRole) && !s4PartnerNo.equals(partnerNo)) {
        patchlist.add(party.PartnerFunction + sparty.FuncnlLocPartnerObjectNmbr + party.Partner)
        existingParty = true
      }

    }
    if (!existingParty) postlist.add(party.PartnerFunction + party.Partner)
  }

  def xmlWriter = new StringWriter()
  def xmlMarkup = new MarkupBuilder(xmlWriter)
  
// FunctionalLocationPartner(FunctionalLocation='{FunctionalLocation}',PartnerFunction='{PartnerFunction}',FuncnlLocPartnerObjectNmbr='{FuncnlLocPartnerObjectNmbr}')
  xmlMarkup.batchParts {
    batchChangeSet {
      for (e in patchlist) {
        batchChangeSetPart {
          method('PATCH')
          uri("FunctionalLocationPartner(FunctionalLocation='" + funLocNo + "',PartnerFunction='" + e[0] +"',FuncnlLocPartnerObjectNmbr='" + e[1] +"')")
          FunctionalLocationPartner {
            FunctionalLocationPartnerType {
              Partner(e[2])
            }
          }
        }

      }
      for (e in postlist) {
        batchChangeSetPart {
          method('POST')
          uri('FunctionalLocationPartner')
          FunctionalLocationPartner {
            FunctionalLocationPartnerType {
              FunctionalLocation(s4_FunctionalLocation)
              PartnerFunction(e[0])
              Partner(e[1])
            }
          }
        }
      }
    }
  }

  String result = xmlWriter.toString()
  message.setBody(result)
  return message

}

/**
 * Creates FunctionalLocation long text batch payload
 * Format : PATCH FunctionalLocationLongText(FunctionalLocation='10006844') HTTP/1.1
 *          
*/

def Message processTextCollection(Message message) {

  
  def map = message.getProperties()
  
  def textContent = map.get("P_LongText")
  def funLocNo = map.get("P_PartnerFunLocNum")
  
  def xmlWriter = new StringWriter()
  def xmlMarkup = new MarkupBuilder(xmlWriter)

  xmlMarkup.batchParts {
    batchChangeSet {
      if (textContent.size() > 0) batchChangeSetPart {
        method('PATCH')
        uri("FunctionalLocationLongText(FunctionalLocation='" + funLocNo + "')")
        FunctionalLocationLongText {
          FunctionalLocationLongTextType {
            FuncnlLocLongText(textContent)
          }
        }
      }
    }
  }

  String result = xmlWriter.toString()
  message.setBody(result)
  return message

}

def Message throwBatchProcessingException(Message message) {
  def body = message.getBody(java.io.Reader)
  def errorBody = null
  try {
    def batchPartResponse = new XmlSlurper().parse(body)
    errorBody = batchPartResponse.batchChangeSetResponse.batchChangeSetPartResponse.body
    
  } catch (Exception ex) {
    throw new Exception('Error During Update of functioanl location via batch call!')
  }

  if (errorBody != null)
    throw new Exception(errorBody.toString())
  else
    throw new Exception('Error During Update of functioanl location via batch call!')
  return message

}

/**
 * For ODATA post error get the error message text from the ODATA response body and populate it in MPL
 * For Other exceptions return as is
*/

def Message throwODATACreateException(Message message) {
    
  // get a map of iflow properties
  def map = message.getProperties();
  def ex = map.get("CamelExceptionCaught")

  if (ex != null ) {
     try {
        def errorBody = new XmlSlurper().parseText(message.getBody(String));
        if (errorBody.message != null) throw new Exception(errorBody.message.text());
     }
     catch(Exception exception) {
        throw new Exception(exception)
     }
  }
  return message
}

/**
 * Check for Application sequencing 
 * If the sender sequence no in the incoming message is older than the sequence no in data store end message processing with status error.
 * Also set custom status in MPL as Obsolete to make it more transparent
 * If the ReceiverInstallationPointID is not available in the incoming message (can happen if message was restarted after a failure), check if a datastore entry for the C4C equipment exists,
 * if yes, then populate the ReceiverInstallationPointID for the PATCH call.
 * The check for sequence no is only required if data store entry is found.
*/

def Message checkAppSequencing(Message message) {

  def dataStoreEntryFound = message.getHeader("SAP_DataStoreEntryFound", String)

  if (dataStoreEntryFound == 'true') {

    def body = message.getBody(java.io.Reader)
    def sequencingData = new JsonSlurper().parse(body)

    map = message.getProperties()
    def currentSequenceNo = map.get("P_SenderSequenceNo")
    def oldSequenceNo = sequencingData.DataStoreCreationDateTime
    message.setProperty("p_DataStoreCreationDateTime", sequencingData.DataStoreCreationDateTime)

    def df = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"

    def oldTimeStamp = Instant.parse(oldSequenceNo)
    def newTimeStamp = Instant.parse(currentSequenceNo)

    if (newTimeStamp.compareTo(oldTimeStamp) < 0) {
      def messageLog = messageLogFactory.getMessageLog(message)
      if (messageLog != null) {
        messageLog.addCustomHeaderProperty("Status", "Obsolete")
      }
      throw new Exception('A newer message has already been processed for the Functional Location ' + sequencingData.InstallationPointID)
    }

    def s4FunLocNo = map.get("P_FunLocNum")
    if (s4FunLocNo.isEmpty()) message.setProperty("P_FunLocNum", sequencingData.ReceiverInstallationPointID)

  }

  return message
}

def Message fillS4FunLocID(Message message)
{
    def body = message.getBody(String)
    def data = new JsonSlurper().parseText(body)
    message.setProperty("P_FunLocNum", data.FunctionalLocation)
    return message
}

def Message setCustomConfirmationMessageHeader(Message message) {
    
  def messageLog = messageLogFactory.getMessageLog(message);
	if(messageLog != null){
       
		def messageHeaderId = message.getProperties().get("P_MessageHeaderId")		
		if(messageHeaderId != null){
			messageLog.addCustomHeaderProperty("Confirmation Message ID", messageHeaderId)
        }
	}
	return message;
}
