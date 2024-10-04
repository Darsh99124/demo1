import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import groovy.xml.XmlParser;

def Message enrichJsonPayload(Message message) {

  def businessPartnerS4ReplicationOutOpenAPISchemaStream = this.getClass().getResourceAsStream('/src/main/resources/json/businessPartnerS4ReplicationOut.json')

  try {

    def schema = new JsonSlurper().parse(businessPartnerS4ReplicationOutOpenAPISchemaStream)
      .components.schemas.BusinessPartnerOutboundReplicationcreaterequest.properties.messageRequests.items.properties.body

    def body = message.getBody(java.io.Reader)
    def payload = new JsonSlurper().parse(body)
    
    def messageId = payload.messageHeader.id
    message.setHeader('SAP_ApplicationID', messageId)
    message.setHeader('SapMessageIdEx', messageId)

    payload.messageRequests.each {
      request ->
        setMissingAttributesToNull(request.body, schema)
    }

    def modifiedJson = JsonOutput.toJson(payload)
    message.setBody(modifiedJson)

  } finally {
    if (businessPartnerS4ReplicationOutOpenAPISchemaStream != null) {
      businessPartnerS4ReplicationOutOpenAPISchemaStream.close()
    }
  }

  return message;
}

void setMissingAttributesToNull(payload, schema) {
  processProperties(schema.properties, payload)

}

void processProperties(schemaProps, payloadProps) {
  schemaProps.each {
    propertyName,
    propertyDetails ->
    if (propertyDetails.type == "object") {
      if (payloadProps[propertyName]) {
        processProperties(propertyDetails.properties, payloadProps[propertyName])
      } else {
        payloadProps[propertyName] = [: ]
        processProperties(propertyDetails.properties, payloadProps[propertyName])
      }
    } else if (propertyDetails.type == "array") {
      if (payloadProps[propertyName]) {
        payloadProps[propertyName].each {
          item ->
            processProperties(propertyDetails.items.properties, item)
        }
      }
    } else if (propertyDetails.type == "boolean" && !payloadProps.containsKey(propertyName)) {
      payloadProps[propertyName] = false
    } else if (!payloadProps.containsKey(propertyName)) {
      payloadProps[propertyName] = null
    }
  }
}

def Message nullValueHandling(Message message) {

	def body = message.getBody(java.io.Reader)
	def xml = new XmlParser().parse(body)

//find tags with null value
    def results = xml.'**'.findAll{it.text() == 'null'}

    results.each{
// //create new node with required attributes, but without value    
    def newNode = new Node(null, it.name(), ['xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance', 'xsi:nil': 'true'] )
// replace node    
    it.replaceNode(newNode)
}
    
// write to body    
    StringWriter stringWriter = new StringWriter()
    XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(stringWriter))
    nodePrinter.setPreserveWhitespace(true)
    nodePrinter.print xml
    
    message.setBody(stringWriter.toString())
   return message;

}

def Message saveSourceMessage(Message message) {
    //Store sourcePayload to be used for custom Exit
    def json = message.getBody(java.io.Reader)
    def data = new JsonSlurper().parse(json)
   
    message.setHeader("sourcePayload", data)
    return message
}