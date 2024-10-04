import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processXmlInput(Message message) {

  def body = message.getBody(java.io.Reader)
  def xml = new XmlParser().parse(body)

  //find tags with null value
  def results = xml.
  '**'.findAll {
    !it.value()
  }

  results.each {
    def newNode = new Node(null, it.name(), 'xsi.nil')
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

def Message enrichJsonOutput(Message message) {
  //Body 
 	//Body 
	def body = message.getBody(String);

	JsonSlurper slurper = new JsonSlurper()
	Map parsedJson = slurper.parseText(body)

	parsedJson.messageRequests.each {
		if (it.body.isDefault) {
			it.body.isDefault = Boolean.parseBoolean(it.body.isDefault)
		}
		it.body.contactPerson.workplaceAddress.each {
		    if (it.isDefault) {
			it.isDefault = Boolean.parseBoolean(it.isDefault)
		}
			it.address.telephone.each {
				if (it.isMobilePhoneNumber) {
					it.isMobilePhoneNumber = Boolean.parseBoolean(it.isMobilePhoneNumber)
				}
				if (it.isUsageDenied) {
					it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
				}
			}

			it.address.facsimile.each {
				if (it.isUsageDenied) {
					it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
				}
			}

			it.address.email.each {
				if (it.isUsageDenied) {
					it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
				}
			}
		}

	}

	def respbody = JsonOutput.toJson(parsedJson)
	// handle xml nil 
    def body2 = respbody.replace('"xsi.nil"','null')
    message.setBody(body2)

	return message
}