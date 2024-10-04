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

  def languageArray = languages.split(',')

  def languageFilter = languageArray.collect {
    "Language%20eq%20%27$it%27"
  }.join('%20or%20')

  def textFilter = "\$filter=" + codeListEntityDesc + "%20ne%20%27%27" + (languages ? ("%20and%20%28" + languageFilter + "%29") : ("")) + (sapClient ? "&sap-client=$sapClient" : "")

  message.setProperty("P_queryFilter", textFilter)

  def textFilter2 = (sapClient ? "sap-client=$sapClient" : "")
  
  message.setProperty("P_sapClient", textFilter2)
    
  return message
}

def Message prepareReceiverPayload(Message message) {

  def properties = message.getProperties()
  def body = message.getBody(java.io.Reader)

  def s4CodeDescriptionList = new JsonSlurper().parse(body).value
  def s4Codes = properties.get("p_s4codes")

  def senderBusinessSystem = properties.get("SenderBusinessSystem")
  def receiverBusinessSystem = properties.get("ReceiverBusinessSystem")

  def s4CodeList = new JsonSlurper().parse(s4Codes).value

  def enricheds4codeList = enrichDescriptions(s4CodeList, s4CodeDescriptionList, "ProductType")

  def responsePayload = getResponsePayload(enricheds4codeList, receiverBusinessSystem, senderBusinessSystem, message)
  

  message.setBody(responsePayload)

  return message
}

def enrichDescriptions(Object s4codeList, Object s4CodeDescriptionList, elementToCompare) {

  def outputList = s4codeList.collect {
    obj1 ->
      def descriptions = s4CodeDescriptionList.findAll {
        obj2 -> obj1[elementToCompare] == obj2[elementToCompare]
      }
    if (descriptions) {
      def reducedDescriptionsList = descriptions.collect {
        e -> ["languageCode": e.Language,
          "content": e.MaterialTypeName
        ]
      }
      obj1["descriptions"] = reducedDescriptionsList
    }
    obj1
  }
  return outputList
}


def generateRandomGUID() {
  return UUID.randomUUID().toString()
}

def getResponsePayload(inputList, String receiverCommunicationSystemDisplayId, String senderCommunicationSystemDisplayId, Message message) {

  def properties = message.getProperties()
  def initialLoad = properties.get("InitialLoad")
    
  def outputList = inputList.collect {
    item ->
    if( item.ProductTypeCode == "" ) {
        item.ProductTypeCode = "1"
      }
    def messageHeader = ["messageEntityName": "sap.crm.md.integrationmetadataservice.entity.productTypeS4ReplicationMessageIn", "actionCode": "SAVE", "id": generateRandomGUID()]
    def body = ["code": item.ProductType,
      "isActive": true,
      "usage":item.ProductTypeCode,
      "descriptions": item.descriptions
    ]

    ["messageHeader": messageHeader, "body": body]
  }
  
    if ('true'.equals(initialLoad)) {
        def cnsCodes = properties.get("p_cnscodes")
        def cnsCodeList = new groovy.json.JsonSlurper().parse(cnsCodes).value

    def codesTobeDeleted = cnsCodeList.findAll {
      cnsCode ->
        !inputList.any {
          it.ProductType == cnsCode.code
        }
    }.collect {it}

    // Add message requests for each item in codesTobeDeleted with action code DELETE
    codesTobeDeleted.each {
      item ->
        def inactiveMessageHeader = ["messageEntityName": "sap.crm.md.integrationmetadataservice.entity.productTypeS4ReplicationMessageIn", "actionCode": "SAVE", "id": generateRandomGUID()]
        def inactiveBody = ["code": item.code, "isActive": false, "usage":item.usage, "descriptions": item.descriptions]
        
       outputList << ["messageHeader": inactiveMessageHeader, "body": inactiveBody]
    }
  }
  
  def creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
  def result = ["messageHeader": [
      "id": generateRandomGUID(),
      "receiverCommunicationSystemDisplayId": "$receiverCommunicationSystemDisplayId", 
      "senderCommunicationSystemDisplayId": "$senderCommunicationSystemDisplayId",
      "creationDateTime": LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))], 
      "messageRequests": outputList]

  return JsonOutput.toJson(result)
}