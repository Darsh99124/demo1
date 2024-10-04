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

  return message
}

def Message prepareReceiverPayload(Message message) {

  def properties = message.getProperties()

  def codeListEntity = properties.get("CodeListEntity")
  def codeListEntityDesc = properties.get("CodeListEntityDesc")

  def body = message.getBody(java.io.Reader)
  def textList = new JsonSlurper().parse(body).value

  def responsePayload = getResponsePayload(textList,
    codeListEntity,
    codeListEntityDesc,
    message)

  message.setBody(responsePayload)

  return message
}

def getResponsePayload(textList, groupByField, descField, message) {

  def properties = message.getProperties()
  def senderBusinessSystem = properties.get("SenderBusinessSystem")
  def receiverBusinessSystem = properties.get("ReceiverBusinessSystem")
  def messageEntityName = properties.get("MessageEntityName")
  def initialLoad = properties.get("InitialLoad")

  def textMap = textList.groupBy {
    text -> text.
    "$groupByField"
  }

  // Add message request for each item in the textMap
  def messageRequests =
    textMap.collect {
      item ->
        def messageHeader = [
          "messageEntityName": messageEntityName,
          "actionCode": "SAVE",
          "id": generateRandomGUID()
        ]
      def requestBody = [
          "code": item.key,
          "descriptions": item.value.collect {
            text -> ["languageCode": text.Language, "content": text.
              "$descField"
            ]
          }
        ]
        ["messageHeader": messageHeader, "body": requestBody]

    }

  if ('true'.equals(initialLoad)) {
    def receiverCodes = message.getHeader("ReceiverCodes", java.io.Reader)
    def receiverCodeList = new groovy.json.JsonSlurper().parse(receiverCodes).value

    def codesTobeDeleted = receiverCodeList.findAll {
      receiverCode ->
        !textList.any {
          it.
          "$groupByField" == receiverCode.code
        }
    }.collect {
      it.code
    }

    // Add message requests for each item in codesTobeDeleted with action code DELETE
    codesTobeDeleted.each {
      it ->
        def deleteMessageHeader = ["messageEntityName": messageEntityName, "actionCode": "DELETE", "id": generateRandomGUID()]
      def deleteRequestBody = ["code": it]
      messageRequests << ["messageHeader": deleteMessageHeader, "body": deleteRequestBody]
    }
  }

  def creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
  def resultMessage = ["messageHeader": ["id": generateRandomGUID(),
      "receiverCommunicationSystemDisplayId": receiverBusinessSystem,
      "senderCommunicationSystemDisplayId": senderBusinessSystem,
      "creationDateTime": "$creationDateTime"
    ],
    "messageRequests": messageRequests
  ]
  return JsonOutput.toJson(resultMessage)
}

def generateRandomGUID() {
  return UUID.randomUUID().toString()
}