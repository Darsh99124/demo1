import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

def Message prepareUnitOfMeasureFilterQuery(Message message) {

  def properties = message.getProperties()
 
  def languages = properties.get("Language")
  def dimensions = properties.get("UnitOfMeasureDimensions")

  def dimensionsArray = dimensions.split(',')
  def languageArray = languages.split(',')

  def dimensionsFilter = dimensionsArray.collect {
    "UnitOfMeasureDimension%20eq%20%27$it%27"
  }.join('%20or%20')
  def languageFilter = languageArray.collect {
    "Language%20eq%20%27$it%27"
  }.join('%20or%20')

  def uomFilter = "\$filter=UnitOfMeasureISOCode%20ne%20%27%27%20" + (dimensions ? "and%20($dimensionsFilter)" : "")
  def isoCodeFilter = "\$filter=UnitOfMeasureISOCodeName%20ne%20%27%27" + (languages ? "and%20($languageFilter)" : "")
  def commercialNameFilter = "\$filter=UnitOfMeasureCommercialName%20ne%20%27%27" + (languages ? "and%20($languageFilter)" : "")

  message.setProperty("P_uomFilter", uomFilter)
  message.setProperty("P_isoCodeFilter", isoCodeFilter)
  message.setProperty("P_commercialNameFilter", commercialNameFilter)

  return message
}

def Message removeDuplicateUOM(Message message) {
  def body = message.getBody(java.io.Reader)

  def parsedData = new JsonSlurper().parse(body)
  def uniqueISOCodes = []
  parsedData.value = parsedData.value.findAll {
    def isoCode = it.UnitOfMeasureISOCode
    if (!uniqueISOCodes.contains(isoCode)) {
      uniqueISOCodes.add(isoCode)
      true
    } else {
      false
    }
  }
  message.setBody(parsedData)
  return message
}

def Message enrichJsonOutputAndSplitIntoChunks(Message message) {

  def properties = message.getProperties()
  def body = message.getBody(java.io.Reader)

  def uomCommercialNameList = new JsonSlurper().parse(body).value
  def uom = properties.get("p_unitofmeasure")
  def isoCodeText = properties.get("p_unitofmeasureisocodetext")

  def senderBusinessSystem = properties.get("SenderBusinessSystem")
  def receiverBusinessSystem = properties.get("ReceiverBusinessSystem")

  def uomList = uom.value
  def isoCodeTextList = new JsonSlurper().parse(isoCodeText).value

  def enrichedUOMList =
    enrichCommercialNames(enrichDescriptions(uomList, isoCodeTextList, "UnitOfMeasureISOCode"), uomCommercialNameList, "UnitOfMeasure")

  def enrichedJson = prepareResponsePayload(enrichedUOMList, receiverBusinessSystem, senderBusinessSystem)
  
    //Split into chunks of 100
  def messageRequests = enrichedJson.messageRequests
  def chunkSize = 100
  def chunks = messageRequests.collate(chunkSize)

  // Wrap chunks inside a new JSON array
  def outputJsonArray = []
  chunks.each {
    chunk ->

      def chunkJson = [
        messageHeader: enrichedJson.messageHeader,
        messageRequests: chunk
      ]

    outputJsonArray << chunkJson
  }

  // Convert to JSON
  def bundledJson = [bundledJson: outputJsonArray]
  def responseBody = JsonOutput.toJson(bundledJson)

  message.setBody(responseBody)
  
  return message
}

def enrichDescriptions(Object uomList, Object isoCodeTextList, elementToCompare) {

  def outputList = uomList.collect {
    obj1 ->
      def descriptions = isoCodeTextList.findAll {
        obj2 -> obj1[elementToCompare] == obj2[elementToCompare]
      }
    if (descriptions) {
      def reducedDescriptionsList = descriptions.collect {
        e -> ["languageCode": e.Language,
          "content": e.UnitOfMeasureISOCodeName
        ]
      }
      obj1["descriptions"] = reducedDescriptionsList
    }
    obj1
  }
  return outputList
}

def enrichCommercialNames(Object uomList, Object uomCommercialNameList, elementToCompare) {

  def outputList = uomList.collect {
    obj1 ->
      def commercialNames = uomCommercialNameList.findAll {
        obj2 -> obj1[elementToCompare] == obj2[elementToCompare]
      }
    if (commercialNames) {
      def reducedDescriptionsList = commercialNames.collect {
        e -> ["languageCode": e.Language,
          "content": e.UnitOfMeasureCommercialName
        ]
      }
      obj1["commercialNames"] = reducedDescriptionsList
    }
    obj1
  }
  return outputList
}

def generateRandomGUID() {
  return UUID.randomUUID().toString()
}

def prepareResponsePayload(inputList, String receiverCommunicationSystemDisplayId, String senderCommunicationSystemDisplayId) {

  def outputList = inputList.collect {
    item ->
      def unitOfMeasureType = item.UnitOfMeasureDimension != "AAAADL" ? "Y" : "X"
    def messageHeader = ["messageEntityName": "sap.crm.md.integrationmetadataservice.entity.unitOfMeasureReplicationMessageIn", "actionCode": "SAVE", "id": generateRandomGUID()]
    def body = ["code": item.UnitOfMeasureISOCode,
      "numberOfDecimals": item.UnitOfMeasureNumberOfDecimals,
      "unitOfMeasureDimension": item.UnitOfMeasureDimension,
      "unitOfMeasureType": unitOfMeasureType,
      "numerator": item.SIUnitCnvrsnRateNumerator,
      "numeratorExponent10": item.SIUnitCnvrsnRateExponent,
      "denominator": item.SIUnitCnvrsnRateDenominator,
      "offset": item.SIUnitCnvrsnAdditiveValue,
      "descriptions": item.descriptions,
      "commercialNames": item.commercialNames
    ]

    ["messageHeader": messageHeader, "body": body]
  }
  
  def result = ["messageHeader": ["receiverCommunicationSystemDisplayId": "$receiverCommunicationSystemDisplayId", "senderCommunicationSystemDisplayId": "$senderCommunicationSystemDisplayId"], "messageRequests": outputList]

  return result
}

def Message prepareResponseMessage(Message message) {

  def body = message.getBody(java.io.Reader)
  def responsePayload = new JsonSlurper().parse(body).bundledJson
  
  responsePayload.messageRequests.each { request ->
    def requestBody = request.body
    requestBody.numberOfDecimals = requestBody.numberOfDecimals.toInteger()
    requestBody.numerator = requestBody.numerator.toInteger()
    requestBody.numeratorExponent10 = requestBody.numeratorExponent10.toInteger()
    requestBody.denominator = requestBody.denominator.toInteger()
    requestBody.offset = requestBody.offset.toBigDecimal() // or toInteger() if offset is always a whole number
}

  responsePayload.messageHeader.id = UUID.randomUUID().toString()
  responsePayload.messageHeader.creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

  def enrichedResponsePayload = JsonOutput.toJson(responsePayload)

  message.setBody(enrichedResponsePayload)
  return message
}