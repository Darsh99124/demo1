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
  def body = message.getBody(java.io.Reader);

  JsonSlurper slurper = new JsonSlurper()
  Map parsedJson = slurper.parse(body)

  def respbody = JsonOutput.toJson(parsedJson)
  // handle xml nil 
  def body2 = respbody.replace('""','null')
  message.setBody(body2)
  
  return message;
}